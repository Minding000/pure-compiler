package components.code_generation.llvm.models.control_flow

import components.code_generation.llvm.models.general.ErrorHandlingContext
import components.code_generation.llvm.models.values.Value
import components.code_generation.llvm.wrapper.LlvmBlock
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.control_flow.IfExpression
import errors.internal.CompilerError

class IfExpression(override val model: IfExpression, val condition: Value, val positiveBranch: ErrorHandlingContext,
				   val negativeBranch: ErrorHandlingContext?): Value(model, listOfNotNull(condition, positiveBranch, negativeBranch)) {

	override fun compile(constructor: LlvmConstructor) {
		val function = constructor.getParentFunction()
		val condition = condition.getLlvmValue(constructor)
		val trueBlock = constructor.createBlock(function, "if_trueBlock")
		val falseBlock = constructor.createBlock(function, "if_falseBlock")
		val exitBlock = constructor.createDetachedBlock("if_exitBlock")
		constructor.buildJump(condition, trueBlock, falseBlock)
		constructor.select(trueBlock)
		positiveBranch.compile(constructor)
		if(!positiveBranch.model.isInterruptingExecutionBasedOnStructure)
			constructor.buildJump(exitBlock)
		constructor.select(falseBlock)
		negativeBranch?.compile(constructor)
		if(negativeBranch?.model?.isInterruptingExecutionBasedOnStructure != true)
			constructor.buildJump(exitBlock)
		if(!model.isInterruptingExecutionBasedOnStructure) {
			constructor.addBlockToFunction(function, exitBlock)
			constructor.select(exitBlock)
		}
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val resultLlvmType = model.effectiveType?.getLlvmType(constructor)
		val result = constructor.buildStackAllocation(resultLlvmType, "if_resultVariable")
		val function = constructor.getParentFunction()
		val condition = model.condition.getLlvmValue(constructor)
		val trueBlock = constructor.createBlock(function, "if_trueBlock")
		val falseBlock = constructor.createBlock(function, "if_falseBlock")
		val exitBlock = constructor.createDetachedBlock("if_exitBlock")
		constructor.buildJump(condition, trueBlock, falseBlock)
		constructor.select(trueBlock)
		compileBranch(constructor, positiveBranch, result, exitBlock)
		constructor.select(falseBlock)
		val negativeBranch = negativeBranch ?: throw CompilerError(model, "If expression is missing a negative branch.")
		compileBranch(constructor, negativeBranch, result, exitBlock)
		if(!model.isInterruptingExecutionBasedOnStructure) {
			constructor.addBlockToFunction(function, exitBlock)
			constructor.select(exitBlock)
		}
		return constructor.buildLoad(resultLlvmType, result, "if_result")
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
					"Last statement in if expression branch block doesn't provide a value.")
				constructor.buildStore(value.getLlvmValue(constructor), result)
			} else {
				statement.compile(constructor)
			}
		}
		constructor.buildJump(exitBlock)
	}
}
