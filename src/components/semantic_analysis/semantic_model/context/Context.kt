package components.semantic_analysis.semantic_model.context

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmType
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.control_flow.LoopStatement
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import logger.Issue
import logger.Logger
import java.util.*

class Context {
	val logger = Logger("compiler")
	val declarationStack = DeclarationStack(logger)
	val surroundingLoops = LinkedList<LoopStatement>()
	lateinit var classDefinitionStruct: LlvmType
	lateinit var functionStruct: LlvmType
	lateinit var llvmMemberOffsetFunction: LlvmValue
	lateinit var llvmMemberOffsetFunctionType: LlvmType
	lateinit var llvmMemberIndexType: LlvmType
	lateinit var llvmMemberIdType: LlvmType
	lateinit var llvmMemberOffsetType: LlvmType
	lateinit var llvmPrintFunctionType: LlvmType
	lateinit var llvmPrintFunction: LlvmValue
	lateinit var llvmExitFunctionType: LlvmType
	lateinit var llvmExitFunction: LlvmValue
	val memberIdentities = IdentityMap<String>()
	val functionSignatures = IdentityMap<String>()


	companion object {
		const val CLASS_DEFINITION_PROPERTY_INDEX = 0
		const val MEMBER_COUNT_PROPERTY_INDEX = 0
		const val MEMBER_ID_ARRAY_PROPERTY_INDEX = 1
		const val MEMBER_OFFSET_ARRAY_PROPERTY_INDEX = 2
	}

	fun addIssue(issue: Issue) = logger.add(issue)

	fun registerWrite(variableDeclaration: Value) {
		if(variableDeclaration !is VariableValue)
			return
		val declaration = variableDeclaration.definition ?: return
		for(surroundingLoop in surroundingLoops) {
			surroundingLoop.mutatedVariables.add(declaration)
		}
	}

	fun resolveMember(constructor: LlvmConstructor, targetType: LlvmType?, targetLocation: LlvmValue, memberIdentifier: String): LlvmValue {
		val classDefinitionAddressLocation = constructor.buildGetPropertyPointer(targetType, targetLocation, CLASS_DEFINITION_PROPERTY_INDEX, "classDefinition")
		val classDefinitionAddress = constructor.buildLoad(
			constructor.createPointerType(classDefinitionStruct),
			classDefinitionAddressLocation,
			"classDefinitionAddress"
		)
		val memberOffset = constructor.buildFunctionCall(
			llvmMemberOffsetFunctionType, llvmMemberOffsetFunction, listOf(
				classDefinitionAddress,
				constructor.buildInt32(memberIdentities.getId(memberIdentifier))
			), "memberIndex"
		)
		return constructor.buildGetArrayElementPointer(constructor.byteType, targetLocation, memberOffset, "memberAddress")
	}

	fun printDebugMessage(constructor: LlvmConstructor, formatString: String, vararg values: LlvmValue) {
		val formatStringGlobal = constructor.buildGlobalCharArray("debugFormat", "$formatString\n")
		constructor.buildFunctionCall(llvmPrintFunctionType, llvmPrintFunction, listOf(formatStringGlobal, *values), "debugPrintCall")
	}
}
