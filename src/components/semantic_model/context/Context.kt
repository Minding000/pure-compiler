package components.semantic_model.context

import code.Main
import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmType
import components.code_generation.llvm.LlvmValue
import components.semantic_model.control_flow.LoopStatement
import components.semantic_model.control_flow.Try
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.general.SemanticModel
import components.semantic_model.types.Type
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue
import logger.Issue
import logger.Logger
import java.util.*
import kotlin.properties.Delegates

class Context {
	val logger = Logger("compiler")
	val declarationStack = DeclarationStack(logger)
	val surroundingLoops = LinkedList<LoopStatement>()
	lateinit var symbolTable: LlvmValue
	lateinit var classDefinitionStruct: LlvmType
	lateinit var llvmConstantOffsetFunction: LlvmValue
	lateinit var llvmPropertyOffsetFunction: LlvmValue
	lateinit var llvmFunctionAddressFunction: LlvmValue
	lateinit var llvmConstantOffsetFunctionType: LlvmType
	lateinit var llvmPropertyOffsetFunctionType: LlvmType
	lateinit var llvmFunctionAddressFunctionType: LlvmType
	lateinit var llvmMemberIndexType: LlvmType
	lateinit var llvmMemberIdType: LlvmType
	lateinit var llvmMemberOffsetType: LlvmType
	lateinit var llvmMemberAddressType: LlvmType
	lateinit var llvmStandardInputStreamGlobal: LlvmValue
	lateinit var llvmStandardOutputStreamGlobal: LlvmValue
	lateinit var llvmStandardErrorStreamGlobal: LlvmValue
	lateinit var llvmPrintFunctionType: LlvmType
	lateinit var llvmPrintFunction: LlvmValue
	lateinit var llvmStreamOpenFunctionType: LlvmType
	lateinit var llvmStreamOpenFunction: LlvmValue
	lateinit var llvmStreamErrorFunctionType: LlvmType
	lateinit var llvmStreamErrorFunction: LlvmValue
	lateinit var llvmStreamCloseFunctionType: LlvmType
	lateinit var llvmStreamCloseFunction: LlvmValue
	lateinit var llvmStreamWriteFunctionType: LlvmType
	lateinit var llvmStreamWriteFunction: LlvmValue
	lateinit var llvmStreamReadByteFunctionType: LlvmType
	lateinit var llvmStreamReadByteFunction: LlvmValue
	lateinit var llvmStreamReadFunctionType: LlvmType
	lateinit var llvmStreamReadFunction: LlvmValue
	lateinit var llvmStreamFlushFunctionType: LlvmType
	lateinit var llvmStreamFlushFunction: LlvmValue
	lateinit var llvmSleepFunctionType: LlvmType
	lateinit var llvmSleepFunction: LlvmValue
	lateinit var llvmExitFunctionType: LlvmType
	lateinit var llvmExitFunction: LlvmValue
	lateinit var llvmMemoryCopyFunctionType: LlvmType
	lateinit var llvmMemoryCopyFunction: LlvmValue
	lateinit var variadicParameterListStruct: LlvmType
	lateinit var llvmVariableParameterIterationStartFunctionType: LlvmType
	lateinit var llvmVariableParameterIterationStartFunction: LlvmValue
	lateinit var llvmVariableParameterListCopyFunctionType: LlvmType
	lateinit var llvmVariableParameterListCopyFunction: LlvmValue
	lateinit var llvmVariableParameterIterationEndFunctionType: LlvmType
	lateinit var llvmVariableParameterIterationEndFunction: LlvmValue
	lateinit var closureStruct: LlvmType
	lateinit var arrayDeclarationType: LlvmType
	lateinit var booleanDeclarationType: LlvmType
	lateinit var byteArrayDeclarationType: LlvmType
	lateinit var byteDeclarationType: LlvmType
	lateinit var integerDeclarationType: LlvmType
	lateinit var floatDeclarationType: LlvmType
	lateinit var nativeInputStreamDeclarationType: LlvmType
	lateinit var nativeOutputStreamDeclarationType: LlvmType
	lateinit var arrayClassDefinition: LlvmValue
	lateinit var booleanClassDefinition: LlvmValue
	lateinit var byteArrayClassDefinition: LlvmValue
	lateinit var byteClassDefinition: LlvmValue
	lateinit var integerClassDefinition: LlvmValue
	lateinit var floatClassDefinition: LlvmValue
	lateinit var nativeInputStreamClassDefinition: LlvmValue
	lateinit var nativeOutputStreamClassDefinition: LlvmValue
	var arrayValueIndex by Delegates.notNull<Int>()
	var booleanValueIndex by Delegates.notNull<Int>()
	var byteArrayValueIndex by Delegates.notNull<Int>()
	var byteValueIndex by Delegates.notNull<Int>()
	var integerValueIndex by Delegates.notNull<Int>()
	var floatValueIndex by Delegates.notNull<Int>()
	var nativeInputStreamValueIndex by Delegates.notNull<Int>()
	var nativeOutputStreamValueIndex by Delegates.notNull<Int>()
	var stringTypeDeclaration: TypeDeclaration? = null
	lateinit var llvmStringByteArrayInitializerType: LlvmType
	lateinit var llvmStringByteArrayInitializer: LlvmValue
	val memberIdentities = IdentityMap<String>()
	val nativeRegistry = NativeRegistry(this)
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

	fun continueRaise(constructor: LlvmConstructor, parent: SemanticModel?) {
		if(parent is Try && parent.isOptional)
			return
		val exceptionParameter = getExceptionParameter(constructor)
		val exception = constructor.buildLoad(constructor.pointerType, exceptionParameter, "exception")
		val doesExceptionExist = constructor.buildIsNotNull(exception, "doesExceptionExist")
		val exceptionBlock = constructor.createBlock("exception")
		val noExceptionBlock = constructor.createBlock("noException")
		constructor.buildJump(doesExceptionExist, exceptionBlock, noExceptionBlock)
		constructor.select(exceptionBlock)
		val returnType = constructor.getReturnType()
		if(returnType == constructor.voidType)
			constructor.buildReturn()
		else
			constructor.buildReturn(getNullValue(constructor, returnType))
		constructor.select(noExceptionBlock)
		//TODO check for handle block
		//TODO check for always block
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

	fun resolveMember(constructor: LlvmConstructor, targetLocation: LlvmValue, memberIdentifier: String,
					  isStaticMember: Boolean = false): LlvmValue {
		val classDefinition = getClassDefinition(constructor, targetLocation)
		val resolutionFunctionType = if(isStaticMember) llvmConstantOffsetFunctionType else llvmPropertyOffsetFunctionType
		val resolutionFunction = if(isStaticMember) llvmConstantOffsetFunction else llvmPropertyOffsetFunction
		val memberOffset = constructor.buildFunctionCall(resolutionFunctionType, resolutionFunction,
			listOf(classDefinition, constructor.buildInt32(memberIdentities.getId(memberIdentifier))), "_memberOffset")
		return constructor.buildGetArrayElementPointer(constructor.byteType, targetLocation, memberOffset, "_memberAddress")
	}

	fun resolveFunction(constructor: LlvmConstructor, targetLocation: LlvmValue, signatureIdentifier: String): LlvmValue {
		val classDefinition = getClassDefinition(constructor, targetLocation)
		return constructor.buildFunctionCall(llvmFunctionAddressFunctionType, llvmFunctionAddressFunction,
			listOf(classDefinition, constructor.buildInt32(memberIdentities.getId(signatureIdentifier))), "_functionAddress")
	}

	fun resolveMemberIdentifier(constructor: LlvmConstructor, memberId: LlvmValue): LlvmValue {
		val memberIdentifierElement = constructor.buildGetArrayElementPointer(constructor.pointerType, symbolTable, memberId,
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
	fun printDebugMessage(constructor: LlvmConstructor, formatString: String, vararg values: LlvmValue) {
		if(Main.shouldPrintRuntimeDebugOutput)
			printMessage(constructor, formatString, *values)
	}

	fun panic(constructor: LlvmConstructor, formatString: String, vararg values: LlvmValue) {
		printMessage(constructor, formatString, *values)
		val exitCode = constructor.buildInt32(1)
		constructor.buildFunctionCall(llvmExitFunctionType, llvmExitFunction, listOf(exitCode))
	}

	fun printMessage(constructor: LlvmConstructor, formatString: String, vararg values: LlvmValue) {
		assert(formatString.count { it == '%' } == values.size) { "Wrong template count!" }

		val formatStringGlobal = constructor.buildGlobalAsciiCharArray("pure_debug_formatString", "$formatString\n")

		val handle = constructor.buildLoad(constructor.pointerType, llvmStandardOutputStreamGlobal, "handle")
		constructor.buildFunctionCall(llvmPrintFunctionType, llvmPrintFunction, listOf(handle, formatStringGlobal, *values))
		constructor.buildFunctionCall(llvmStreamFlushFunctionType, llvmStreamFlushFunction, listOf(handle))
	}
}
