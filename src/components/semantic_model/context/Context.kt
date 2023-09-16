package components.semantic_model.context

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmType
import components.code_generation.llvm.LlvmValue
import components.semantic_model.control_flow.LoopStatement
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue
import logger.Issue
import logger.Logger
import java.util.*

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
	lateinit var llvmExitFunctionType: LlvmType
	lateinit var llvmExitFunction: LlvmValue
	lateinit var variadicParameterListStruct: LlvmType
	lateinit var llvmVariableParameterIterationStartFunctionType: LlvmType
	lateinit var llvmVariableParameterIterationStartFunction: LlvmValue
	lateinit var llvmVariableParameterListCopyFunctionType: LlvmType
	lateinit var llvmVariableParameterListCopyFunction: LlvmValue
	lateinit var llvmVariableParameterIterationEndFunctionType: LlvmType
	lateinit var llvmVariableParameterIterationEndFunction: LlvmValue
	val memberIdentities = IdentityMap<String>()


	companion object {
		const val CLASS_DEFINITION_PROPERTY_INDEX = 0
		const val CONSTANT_COUNT_PROPERTY_INDEX = 0
		const val CONSTANT_ID_ARRAY_PROPERTY_INDEX = 1
		const val CONSTANT_OFFSET_ARRAY_PROPERTY_INDEX = 2
		const val PROPERTY_COUNT_PROPERTY_INDEX = 3
		const val PROPERTY_ID_ARRAY_PROPERTY_INDEX = 4
		const val PROPERTY_OFFSET_ARRAY_PROPERTY_INDEX = 5
		const val FUNCTION_COUNT_PROPERTY_INDEX = 6
		const val FUNCTION_ID_ARRAY_PROPERTY_INDEX = 7
		const val FUNCTION_ADDRESS_ARRAY_PROPERTY_INDEX = 8
		const val THIS_PARAMETER_INDEX = 0
	}

	fun addIssue(issue: Issue) = logger.add(issue)

	fun registerWrite(variableDeclaration: Value) {
		if(variableDeclaration !is VariableValue)
			return
		val declaration = variableDeclaration.declaration ?: return
		for(surroundingLoop in surroundingLoops)
			surroundingLoop.mutatedVariables.add(declaration)
	}

	fun resolveMember(constructor: LlvmConstructor, targetType: LlvmType?, targetLocation: LlvmValue, memberIdentifier: String, isStaticMember: Boolean = false): LlvmValue {
		val classDefinitionAddressLocation = constructor.buildGetPropertyPointer(targetType, targetLocation,
			CLASS_DEFINITION_PROPERTY_INDEX, "classDefinition")
		val classDefinitionAddress = constructor.buildLoad(
			constructor.pointerType,
			classDefinitionAddressLocation, "classDefinitionAddress"
		)
		val resolutionFunctionType = if(isStaticMember) llvmConstantOffsetFunctionType else llvmPropertyOffsetFunctionType
		val resolutionFunction = if(isStaticMember) llvmConstantOffsetFunction else llvmPropertyOffsetFunction
		val memberOffset = constructor.buildFunctionCall(resolutionFunctionType, resolutionFunction, listOf(
				classDefinitionAddress, constructor.buildInt32(memberIdentities.getId(memberIdentifier))), "memberOffset")
		return constructor.buildGetArrayElementPointer(constructor.byteType, targetLocation, memberOffset, "memberAddress")
	}

	fun getThisParameter(constructor: LlvmConstructor): LlvmValue {
		return constructor.getParameter(constructor.getParentFunction(),
			THIS_PARAMETER_INDEX
		)
	}

	fun printDebugMessage(constructor: LlvmConstructor, formatString: String, vararg values: LlvmValue) {
		val formatStringGlobal = constructor.buildGlobalCharArray("debugFormat", "$formatString\n")
		constructor.buildFunctionCall(llvmPrintFunctionType, llvmPrintFunction, listOf(formatStringGlobal, *values), "debugPrintCall")
	}
}
