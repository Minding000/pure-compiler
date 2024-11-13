package components.code_generation.llvm.models.control_flow

import components.code_generation.llvm.models.general.ErrorHandlingContext
import components.code_generation.llvm.models.values.Value
import components.code_generation.llvm.wrapper.LlvmBlock
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.control_flow.SwitchExpression
import errors.internal.CompilerError
import java.util.*

class SwitchExpression(override val model: SwitchExpression, val subject: Value, val cases: List<Case>,
					   val elseBranch: ErrorHandlingContext?): Value(model, listOfNotNull(subject, *cases.toTypedArray(), elseBranch)) {

	override fun compile(constructor: LlvmConstructor) {
		val function = constructor.getParentFunction()
		val elseBlock = constructor.createBlock(function, "switch_elseBlock")
		val exitBlock = constructor.createDetachedBlock("switch_exitBlock")
		if(cases.isNotEmpty()) {
			val targetBlocks = LinkedList<LlvmBlock>()
			for(case in cases)
				targetBlocks.add(constructor.createBlock(function, "switch_caseConditionBlock"))
			targetBlocks.add(elseBlock)
			val subjectValue = subject.getLlvmValue(constructor)
			constructor.buildJump(targetBlocks.first())
			for((caseIndex, case) in cases.withIndex()) {
				val condition = buildEquals(constructor, case.condition.getLlvmValue(constructor), subjectValue)
				val currentConditionBlock = targetBlocks[caseIndex]
				val nextTargetBlock = targetBlocks[caseIndex + 1]
				constructor.select(currentConditionBlock)
				val caseBodyBlock = constructor.createBlock(function, "switch_caseBodyBlock")
				constructor.buildJump(condition, caseBodyBlock, nextTargetBlock)
				constructor.select(caseBodyBlock)
				case.result.compile(constructor)
				if(!case.result.model.isInterruptingExecutionBasedOnStructure)
					constructor.buildJump(exitBlock)
			}
			constructor.select(elseBlock)
		}
		if(elseBranch == null) {
			if(model.isInterruptingExecutionBasedOnStructure) {
				context.panic(constructor, "Exhaustive switch statement did not match any case!")
				constructor.markAsUnreachable()
			}
		} else {
			elseBranch.compile(constructor)
		}
		if(!model.isInterruptingExecutionBasedOnStructure) {
			if(elseBranch?.model?.isInterruptingExecutionBasedOnStructure != true)
				constructor.buildJump(exitBlock)
			constructor.addBlockToFunction(function, exitBlock)
			constructor.select(exitBlock)
		}
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val resultLlvmType = model.effectiveType?.getLlvmType(constructor)
		val result = constructor.buildStackAllocation(resultLlvmType, "switch_resultVariable")
		val function = constructor.getParentFunction()
		val elseBlock = constructor.createBlock(function, "switch_elseBlock")
		val exitBlock = constructor.createDetachedBlock("switch_exitBlock")
		if(cases.isNotEmpty()) {
			val targetBlocks = LinkedList<LlvmBlock>()
			for(case in cases)
				targetBlocks.add(constructor.createBlock(function, "switch_caseConditionBlock"))
			targetBlocks.add(elseBlock)
			val subjectValue = subject.getLlvmValue(constructor)
			constructor.buildJump(targetBlocks.first())
			for((caseIndex, case) in cases.withIndex()) {
				val condition = buildEquals(constructor, case.condition.getLlvmValue(constructor), subjectValue)
				val currentConditionBlock = targetBlocks[caseIndex]
				val nextTargetBlock = targetBlocks[caseIndex + 1]
				constructor.select(currentConditionBlock)
				val caseBodyBlock = constructor.createBlock(function, "switch_caseBodyBlock")
				constructor.buildJump(condition, caseBodyBlock, nextTargetBlock)
				constructor.select(caseBodyBlock)
				compileBranch(constructor, case.result, result, exitBlock)
			}
			constructor.select(elseBlock)
		}
		if(elseBranch == null) {
			if(model.isInterruptingExecutionBasedOnStructure) {
				context.panic(constructor, "Exhaustive switch statement did not match any case!")
				constructor.markAsUnreachable()
			} else {
				constructor.buildJump(exitBlock)
			}
		} else {
			compileBranch(constructor, elseBranch, result, exitBlock)
		}
		if(!model.isInterruptingExecutionBasedOnStructure) {
			constructor.addBlockToFunction(function, exitBlock)
			constructor.select(exitBlock)
		}
		return constructor.buildLoad(resultLlvmType, result, "switch_result")
	}

	private fun buildEquals(constructor: LlvmConstructor, leftValue: LlvmValue, rightValue: LlvmValue): LlvmValue {
		//TODO this check needs to work for any type
		return constructor.buildBooleanEqualTo(leftValue, rightValue, "switch_case_condition_result")
	}

	private fun compileBranch(constructor: LlvmConstructor, branch: ErrorHandlingContext, result: LlvmValue, exitBlock: LlvmBlock) {
		if(branch.model.isInterruptingExecutionBasedOnStructure) {
			branch.compile(constructor)
			return
		}
		val statements = branch.mainBlock.statements
		val lastStatementIndex = statements.size - 1
		for((statementIndex, statement) in statements.withIndex()) {
			if(statementIndex == lastStatementIndex) {
				val value = statement as? Value ?: throw CompilerError(statement.model,
					"Last statement in switch expression branch block doesn't provide a value.")
				constructor.buildStore(value.getLlvmValue(constructor), result)
			} else {
				statement.compile(constructor)
			}
		}
		constructor.buildJump(exitBlock)
	}
}
