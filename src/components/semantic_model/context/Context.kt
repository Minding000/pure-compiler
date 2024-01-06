package components.semantic_model.context

import code.Main
import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmType
import components.code_generation.llvm.LlvmValue
import components.code_generation.llvm.native_implementations.*
import components.semantic_model.control_flow.LoopStatement
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue
import errors.internal.CompilerError
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
	var arrayTypeDeclaration: TypeDeclaration? = null
	var booleanTypeDeclaration: TypeDeclaration? = null
	var byteTypeDeclaration: TypeDeclaration? = null
	var integerTypeDeclaration: TypeDeclaration? = null
	var floatTypeDeclaration: TypeDeclaration? = null
	var arrayValueIndex by Delegates.notNull<Int>()
	var booleanValueIndex by Delegates.notNull<Int>()
	var byteValueIndex by Delegates.notNull<Int>()
	var integerValueIndex by Delegates.notNull<Int>()
	var floatValueIndex by Delegates.notNull<Int>()
	var stringTypeDeclaration: TypeDeclaration? = null
	lateinit var llvmStringByteArrayInitializerType: LlvmType
	lateinit var llvmStringByteArrayInitializer: LlvmValue
	val memberIdentities = IdentityMap<String>()
	private val nativeInstances = HashMap<String, (constructor: LlvmConstructor) -> LlvmValue>()
	private val nativeImplementations = HashMap<String, (constructor: LlvmConstructor, llvmValue: LlvmValue) -> Unit>()

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

	fun continueRaise() {
		//TODO if exception exists
		// check for optional try (normal and force try have no effect)
		// check for catch
		// resume raise
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

	private fun getClassDefinition(constructor: LlvmConstructor, targetObject: LlvmValue): LlvmValue {
		// The class definition property is the first property, so it can be accessed without GEP
		val classDefinitionProperty = targetObject
		return constructor.buildLoad(constructor.pointerType, classDefinitionProperty, "_classDefinition")
	}

	fun loadNativeImplementations() {
		ArrayNatives.load(this)
		BoolNatives.load(this)
		ByteNatives.load(this)
		CliNatives.load(this)
		FloatNatives.load(this)
		IdentifiableNatives.load(this)
		IntNatives.load(this)
		NullNatives.load(this)
	}

	fun registerNativeInstance(identifier: String, instance: (constructor: LlvmConstructor) -> LlvmValue) {
		val existingInstance = nativeInstances.putIfAbsent(identifier, instance)
		if(existingInstance != null)
			throw CompilerError("Duplicate native instance for identifier '$identifier'.")
	}

	fun getNativeInstanceValue(constructor: LlvmConstructor, identifier: String): LlvmValue {
		val getInstanceValue = nativeInstances[identifier]
			?: throw CompilerError("Missing native instance for identifier '$identifier'.")
		return getInstanceValue(constructor)
	}

	fun registerNativeImplementation(identifier: String, implementation: (constructor: LlvmConstructor, llvmValue: LlvmValue) -> Unit) {
		val existingImplementation = nativeImplementations.putIfAbsent(identifier, implementation)
		if(existingImplementation != null)
			throw CompilerError("Duplicate native implementation for identifier '$identifier'.")
	}

	fun compileNativeImplementation(constructor: LlvmConstructor, identifier: String, llvmValue: LlvmValue) {
		val compileImplementation = nativeImplementations[identifier]
			?: throw CompilerError("Missing native implementation for identifier '$identifier'.")
		compileImplementation(constructor, llvmValue)
	}

	fun printDebugMessage(message: String) {
		if(Main.shouldPrintCompileTimeDebugOutput)
			println(message)
	}

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
