package components.semantic_model.context

import code.Main
import components.code_generation.llvm.ExternalFunctions
import components.code_generation.llvm.StandardLibrary
import components.code_generation.llvm.context.IdentityMap
import components.code_generation.llvm.context.NativeRegistry
import components.code_generation.llvm.runtime_definitions.RuntimeFunctions
import components.code_generation.llvm.runtime_definitions.RuntimeGlobals
import components.code_generation.llvm.runtime_definitions.RuntimeStructs
import components.code_generation.llvm.runtime_definitions.RuntimeTypes
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmFunction
import components.code_generation.llvm.wrapper.LlvmType
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.control_flow.LoopStatement
import components.semantic_model.control_flow.Try
import components.semantic_model.declarations.ComputedPropertyDeclaration
import components.semantic_model.declarations.FunctionImplementation
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.general.SemanticModel
import components.semantic_model.operations.FunctionCall
import components.semantic_model.types.Type
import components.semantic_model.values.Operator
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue
import errors.internal.CompilerError
import logger.Issue
import logger.Logger
import util.count
import java.util.*

class Context {
	val logger = Logger("compiler")
	val declarationStack = DeclarationStack(logger)
	val surroundingLoops = LinkedList<LoopStatement>()
	val memberIdentities = IdentityMap<String>()
	val externalFunctions = ExternalFunctions()
	val runtimeTypes = RuntimeTypes()
	val runtimeStructs = RuntimeStructs()
	val runtimeGlobals = RuntimeGlobals()
	val runtimeFunctions = RuntimeFunctions()
	val standardLibrary = StandardLibrary()
	val nativeRegistry = NativeRegistry(this)
	val constantCharArrayGlobals = HashMap<Pair<String, Boolean>, LlvmValue>()
	/**  While set, the selected primitive will be treated as a wrapped primitive object */
	var primitiveCompilationTarget: TypeDeclaration? = null

	companion object {
		const val CLASS_DEFINITION_PROPERTY_INDEX = 0
		const val PARENT_PROPERTY_INDEX = 1
		const val CONSTANT_COUNT_PROPERTY_INDEX = 0
		const val CONSTANT_ID_ARRAY_PROPERTY_INDEX = 1
		const val CONSTANT_OFFSET_ARRAY_PROPERTY_INDEX = 2
		const val PROPERTY_COUNT_PROPERTY_INDEX = 3
		const val PROPERTY_ID_ARRAY_PROPERTY_INDEX = 4
		const val PROPERTY_OFFSET_ARRAY_PROPERTY_INDEX = 5
		const val FUNCTION_COUNT_PROPERTY_INDEX = 6
		const val FUNCTION_ID_ARRAY_PROPERTY_INDEX = 7
		const val FUNCTION_ADDRESS_ARRAY_PROPERTY_INDEX = 8
		const val EXCEPTION_PARAMETER_INDEX = 0
		const val THIS_PARAMETER_INDEX = 1
		const val PARENT_PARAMETER_OFFSET = 2
		const val VALUE_PARAMETER_OFFSET = 2
		const val CLOSURE_FUNCTION_ADDRESS_PROPERTY_INDEX = 0
	}

	fun addIssue(issue: Issue) = logger.add(issue)

	fun registerWrite(variableDeclaration: Value) {
		if(variableDeclaration !is VariableValue)
			return
		val declaration = variableDeclaration.declaration ?: return
		for(surroundingLoop in surroundingLoops)
			surroundingLoop.mutatedVariables.add(declaration)
	}

	fun getExceptionParameter(constructor: LlvmConstructor, function: LlvmValue = constructor.getParentFunction()): LlvmValue {
		return constructor.getParameter(function, EXCEPTION_PARAMETER_INDEX)
	}

	fun getThisParameter(constructor: LlvmConstructor, function: LlvmValue = constructor.getParentFunction()): LlvmValue {
		return constructor.getParameter(function, THIS_PARAMETER_INDEX)
	}

	//TODO accept Unit as second parameter instead of SemanticModel
	fun continueRaise(constructor: LlvmConstructor, model: SemanticModel) {
		val parent = model.parent
		if(parent is Try && parent.isOptional)
			return
		val exceptionParameter = getExceptionParameter(constructor)
		val exception = constructor.buildLoad(constructor.pointerType, exceptionParameter, "exception")
		val doesExceptionExist = constructor.buildIsNotNull(exception, "doesExceptionExist")
		val exceptionBlock = constructor.createBlock("exception")
		val noExceptionBlock = constructor.createBlock("noException")
		constructor.buildJump(doesExceptionExist, exceptionBlock, noExceptionBlock)
		constructor.select(exceptionBlock)
		val surroundingCallable =
			model.scope.getSurroundingInitializer() ?: model.scope.getSurroundingFunction() ?: model.scope.getSurroundingComputedProperty()
		if(surroundingCallable != null)
			addLocationToStacktrace(model, constructor, exception, surroundingCallable)
		handleException(constructor, parent)
		constructor.select(noExceptionBlock)
	}

	fun addLocationToStacktrace(model: SemanticModel, constructor: LlvmConstructor, exception: LlvmValue,
								surroundingCallable: SemanticModel) {
		if(!nativeRegistry.has(SpecialType.EXCEPTION))
			return
		val line = model.source.start.line
		val moduleName = line.file.module.localName
		val fileName = line.file.name
		val description = if(surroundingCallable is ComputedPropertyDeclaration) {
			val getter = surroundingCallable.getterErrorHandlingContext
			if(getter != null && model.isIn(getter))
				"get $surroundingCallable"
			else
				"set $surroundingCallable"
		} else if(surroundingCallable is FunctionImplementation) {
			val parentFunction = surroundingCallable.parentFunction
			if(parentFunction is Operator)
				surroundingCallable.signature.toString(false, parentFunction.kind)
			else
				surroundingCallable.toString()
		} else {
			surroundingCallable.toString()
		}
		val moduleNameBytes = getConstantCharArrayGlobal(constructor, moduleName, false)
		val moduleNameLength = constructor.buildInt32(moduleName.length)
		val fileNameBytes = getConstantCharArrayGlobal(constructor, fileName, false)
		val fileNameLength = constructor.buildInt32(fileName.length)
		val descriptionBytes = getConstantCharArrayGlobal(constructor, description, false)
		val descriptionLength = constructor.buildInt32(description.length)
		val lineNumber = constructor.buildInt32(line.number)
		constructor.buildFunctionCall(runtimeFunctions.addExceptionLocation,
			listOf(exception, moduleNameBytes, moduleNameLength, fileNameBytes, fileNameLength, descriptionBytes, descriptionLength,
				lineNumber))
	}

	fun handleException(constructor: LlvmConstructor, parent: SemanticModel?) {
		val result = parent?.scope?.getSurroundingErrorHandlingContext()
		val errorHandlingContext = result?.first
		if(errorHandlingContext?.needsToBeCalled() == true) {
			errorHandlingContext.unit.jumpTo(constructor, result.second)
		} else {
			val returnType = constructor.getReturnType()
			if(returnType == constructor.voidType)
				constructor.buildReturn()
			else
				constructor.buildReturn(getNullValue(constructor, returnType))
		}
	}

	fun raiseException(constructor: LlvmConstructor, model: SemanticModel, description: String, isNativeCall: Boolean = false) {
		val exceptionParameter = getExceptionParameter(constructor)
		val descriptionString = createStringObject(constructor, description, exceptionParameter)
		raiseException(constructor, model, descriptionString, isNativeCall)
	}

	fun raiseException(constructor: LlvmConstructor, model: SemanticModel, descriptionString: LlvmValue, isNativeCall: Boolean = false) {
		val parent = model.parent
		val exceptionParameter = getExceptionParameter(constructor)
		val exceptionTypeDeclaration = standardLibrary.exceptionTypeDeclaration
		val exception = constructor.buildHeapAllocation(exceptionTypeDeclaration.llvmType, "exception")
		val exceptionClassDefinitionProperty =
			constructor.buildGetPropertyPointer(exceptionTypeDeclaration.llvmType, exception,
				CLASS_DEFINITION_PROPERTY_INDEX, "exceptionClassDefinitionProperty")
		constructor.buildStore(exceptionTypeDeclaration.llvmClassDefinition, exceptionClassDefinitionProperty)
		if(!exceptionTypeDeclaration.commonClassPreInitializer.isNoop)
			constructor.buildFunctionCall(exceptionTypeDeclaration.commonClassPreInitializer, listOf(exceptionParameter, exception))
		val parameters = listOf(exceptionParameter, exception, descriptionString)
		constructor.buildFunctionCall(standardLibrary.exceptionDescriptionInitializer, parameters)

		if(isNativeCall) {
			if(model !is FunctionCall)
				throw CompilerError(model, "Unknown native call type: ${model.javaClass.simpleName}")
			val nativeDeclaration =
				model.targetSignature ?: model.targetInitializer ?: throw CompilerError(model, "Native call target is missing")
			addLocationToStacktrace(nativeDeclaration, constructor, exception, nativeDeclaration)
		}
		val surroundingCallable =
			model.scope.getSurroundingInitializer() ?: model.scope.getSurroundingFunction() ?: model.scope.getSurroundingComputedProperty()
		if(surroundingCallable != null)
			addLocationToStacktrace(model, constructor, exception, surroundingCallable)
		constructor.buildStore(exception, exceptionParameter)
		handleException(constructor, parent)
	}

	fun raiseOnOverflow(constructor: LlvmConstructor, model: SemanticModel, targetValue: LlvmValue, modifierValue: LlvmValue,
						function: LlvmFunction, exceptionDescription: String, resultName: String): LlvmValue {
		val aggregateResult = constructor.buildFunctionCall(function, listOf(targetValue, modifierValue), "aggregateResult")
		val result = constructor.extractValueFromAggregateValue(aggregateResult, 0, resultName)
		val didOverflow = constructor.extractValueFromAggregateValue(aggregateResult, 1, "didOverflow")
		val overflowBlock = constructor.createBlock("overflow")
		val noOverflowBlock = constructor.createBlock("noOverflow")
		constructor.buildJump(didOverflow, overflowBlock, noOverflowBlock)
		constructor.select(overflowBlock)
		if(nativeRegistry.has(SpecialType.EXCEPTION)) {
			raiseException(constructor, model, exceptionDescription)
		} else {
			panic(constructor, exceptionDescription)
			constructor.markAsUnreachable()
		}
		constructor.select(noOverflowBlock)
		return result
	}

	fun getNullValue(constructor: LlvmConstructor, type: Type?): LlvmValue {
		return if(SpecialType.BOOLEAN.matches(type))
			constructor.buildBoolean(false)
		else if(SpecialType.BYTE.matches(type))
			constructor.buildByte(0)
		else if(SpecialType.INTEGER.matches(type))
			constructor.buildInt32(0)
		else if(SpecialType.FLOAT.matches(type))
			constructor.buildFloat(0.0)
		else
			constructor.nullPointer
	}

	fun getNullValue(constructor: LlvmConstructor, type: LlvmType): LlvmValue {
		return when(type) {
			constructor.booleanType -> constructor.buildBoolean(false)
			constructor.byteType -> constructor.buildByte(0)
			constructor.i32Type -> constructor.buildInt32(0)
			constructor.floatType -> constructor.buildFloat(0.0)
			else -> constructor.nullPointer
		}
	}

	fun getConstantCharArrayGlobal(constructor: LlvmConstructor, content: String, shouldNullTerminate: Boolean = true): LlvmValue {
		return constantCharArrayGlobals.getOrPut(content to shouldNullTerminate) {
			constructor.buildGlobalAsciiCharArray("_constantCharArray", content, shouldNullTerminate)
		}
	}

	fun createStringObject(constructor: LlvmConstructor, content: String, exceptionParameter: LlvmValue): LlvmValue {
		val charArray = getConstantCharArrayGlobal(constructor, content, false)
		val length = constructor.buildInt32(content.length)
		return constructor.buildFunctionCall(runtimeFunctions.createString, listOf(exceptionParameter, charArray, length), "_string")
	}

	fun resolveMember(constructor: LlvmConstructor, targetLocation: LlvmValue, memberIdentifier: String,
					  isStaticMember: Boolean = false): LlvmValue {
		val classDefinition = getClassDefinition(constructor, targetLocation)
		val resolutionFunction = if(isStaticMember) runtimeFunctions.constantOffsetResolution else runtimeFunctions.propertyOffsetResolution
		val memberOffset = constructor.buildFunctionCall(resolutionFunction,
			listOf(classDefinition, constructor.buildInt32(memberIdentities.getId(memberIdentifier))), "_memberOffset")
		return constructor.buildGetArrayElementPointer(constructor.byteType, targetLocation, memberOffset, "_memberAddress")
	}

	fun resolveFunction(constructor: LlvmConstructor, targetLocation: LlvmValue, signatureIdentifier: String): LlvmValue {
		val classDefinition = getClassDefinition(constructor, targetLocation)
		return constructor.buildFunctionCall(runtimeFunctions.functionAddressResolution,
			listOf(classDefinition, constructor.buildInt32(memberIdentities.getId(signatureIdentifier))), "_functionAddress")
	}

	fun resolveMemberIdentifier(constructor: LlvmConstructor, memberId: LlvmValue): LlvmValue {
		val memberIdentifierElement = constructor.buildGetArrayElementPointer(constructor.pointerType, runtimeGlobals.symbolTable, memberId,
			"_memberIdentifierElement")
		return constructor.buildLoad(constructor.pointerType, memberIdentifierElement, "_memberIdentifier")
	}

	fun getClassDefinition(constructor: LlvmConstructor, targetObject: LlvmValue): LlvmValue {
		// The class definition property is the first property, so it can be accessed without GEP
		val classDefinitionProperty = targetObject
		return constructor.buildLoad(constructor.pointerType, classDefinitionProperty, "_classDefinition")
	}

	fun printDebugMessage(message: String) {
		if(Main.shouldPrintCompileTimeDebugOutput)
			println(message)
	}

	/**
	 * Prints a debug message at runtime.
	 * Use '%i' as a placeholder for an integer.
	 * Use '%p' as a placeholder for a pointer.
	 * Documentation: https://en.cppreference.com/w/cpp/io/c/fprintf
	 */
	fun printDebugLine(constructor: LlvmConstructor, formatString: String, vararg values: LlvmValue) {
		if(Main.shouldPrintRuntimeDebugOutput)
			printLine(constructor, formatString, *values)
	}

	fun panic(constructor: LlvmConstructor, formatString: String, vararg values: LlvmValue) {
		printLine(constructor, formatString, *values)
		val exitCode = constructor.buildInt32(1)
		constructor.buildFunctionCall(externalFunctions.exit, listOf(exitCode))
	}

	fun printLine(constructor: LlvmConstructor, formatString: String, vararg values: LlvmValue) {
		printMessage(constructor, "$formatString\n", *values)
	}

	fun printMessage(constructor: LlvmConstructor, formatString: String, vararg values: LlvmValue) {
		val requiredParameterCount = formatString.count('%') + formatString.count("%.*")
		assert(requiredParameterCount == values.size) { "Wrong template count! Expected $requiredParameterCount but got ${values.size}" }

		val formatStringGlobal = constructor.buildGlobalAsciiCharArray("pure_debug_formatString", formatString)

		val handle = constructor.buildLoad(constructor.pointerType, runtimeGlobals.standardOutputStream, "handle")
		constructor.buildFunctionCall(externalFunctions.print, listOf(handle, formatStringGlobal, *values))
		constructor.buildFunctionCall(externalFunctions.streamFlush, listOf(handle))
	}
}
