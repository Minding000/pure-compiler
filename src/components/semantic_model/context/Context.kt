package components.semantic_model.context

import code.Main
import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmType
import components.code_generation.llvm.LlvmValue
import components.semantic_model.control_flow.LoopStatement
import components.semantic_model.declarations.TypeDeclaration
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
	lateinit var llvmPrintFunctionType: LlvmType
	lateinit var llvmPrintFunction: LlvmValue
	lateinit var llvmWriteFunctionType: LlvmType
	lateinit var llvmWriteFunction: LlvmValue
	lateinit var llvmReadFunctionType: LlvmType
	lateinit var llvmReadFunction: LlvmValue
	lateinit var llvmFlushFunctionType: LlvmType
	lateinit var llvmFlushFunction: LlvmValue
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
	lateinit var byteDeclarationType: LlvmType
	lateinit var integerDeclarationType: LlvmType
	lateinit var floatDeclarationType: LlvmType
	lateinit var arrayClassDefinition: LlvmValue
	lateinit var booleanClassDefinition: LlvmValue
	lateinit var byteClassDefinition: LlvmValue
	lateinit var integerClassDefinition: LlvmValue
	lateinit var floatClassDefinition: LlvmValue
	var arrayValueIndex by Delegates.notNull<Int>()
	var booleanValueIndex by Delegates.notNull<Int>()
	var byteValueIndex by Delegates.notNull<Int>()
	var integerValueIndex by Delegates.notNull<Int>()
	var floatValueIndex by Delegates.notNull<Int>()
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

	fun continueRaise(constructor: LlvmConstructor/*, returnType: Type?*/) {
		//TODO if exception exists
		// check for optional try (normal and force try have no effect)
		// check for catch
		// resume raise
//		if(SpecialType.NOTHING.matches(returnType))
//			constructor.buildReturn()
//		else
//			constructor.buildReturn(getNullValue(constructor, returnType))
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

	private fun printMessage(constructor: LlvmConstructor, formatString: String, vararg values: LlvmValue) {
		val formatStringGlobal = constructor.buildGlobalAsciiCharArray("pure_debug_formatString", "$formatString\n")
		constructor.buildFunctionCall(llvmPrintFunctionType, llvmPrintFunction, listOf(formatStringGlobal, *values))
		constructor.buildFunctionCall(llvmFlushFunctionType, llvmFlushFunction, listOf(constructor.nullPointer))
	}
}
