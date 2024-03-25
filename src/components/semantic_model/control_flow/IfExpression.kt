package components.semantic_model.control_flow

import components.code_generation.llvm.LlvmBlock
import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.VariableTracker
import components.semantic_model.general.ErrorHandlingContext
import components.semantic_model.scopes.Scope
import components.semantic_model.types.Type
import components.semantic_model.values.BooleanLiteral
import components.semantic_model.values.Value
import errors.internal.CompilerError
import logger.issues.expressions.BranchMissesValue
import logger.issues.expressions.ExpressionMissesElse
import logger.issues.expressions.ExpressionNeverReturns
import util.combineOrUnion
import java.util.*
import components.syntax_parser.syntax_tree.control_flow.IfExpression as IfStatementSyntaxTree

class IfExpression(override val source: IfStatementSyntaxTree, scope: Scope, val condition: Value, val positiveBranch: ErrorHandlingContext,
				   val negativeBranch: ErrorHandlingContext?, val isPartOfExpression: Boolean): Value(source, scope) {
	override var isInterruptingExecutionBasedOnStructure = false
	override var isInterruptingExecutionBasedOnStaticEvaluation = false
	private var isConditionAlwaysTrue = false
	private var isConditionAlwaysFalse = false

	companion object {
		const val EXPRESSION_TYPE = "if"
	}

	init {
		addSemanticModels(condition, positiveBranch, negativeBranch)
	}

	override fun determineTypes() {
		super.determineTypes()
		if(!isPartOfExpression)
			return
		val types = LinkedList<Type>()
		val positiveBranchType = positiveBranch.getValue()?.providedType
		if(positiveBranchType != null)
			types.add(positiveBranchType)
		if(negativeBranch != null) {
			val negativeBranchType = negativeBranch.getValue()?.providedType
			if(negativeBranchType != null)
				types.add(negativeBranchType)
		}
		if(types.isNotEmpty())
			providedType = types.combineOrUnion(this)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		condition.analyseDataFlow(tracker)
		tracker.setVariableStates(condition.getPositiveEndState())
		positiveBranch.analyseDataFlow(tracker)
		if(negativeBranch == null) {
			tracker.addVariableStates(condition.getNegativeEndState())
		} else {
			val positiveBranchState = tracker.currentState.copy()
			tracker.setVariableStates(condition.getNegativeEndState())
			negativeBranch.analyseDataFlow(tracker)
			tracker.addVariableStates(positiveBranchState)
		}
		(condition.getComputedValue() as? BooleanLiteral)?.value.let { staticResult ->
			isConditionAlwaysTrue = staticResult == true
			isConditionAlwaysFalse = staticResult == false
		}
		if(isPartOfExpression)
			computeStaticValue()
		evaluateExecutionFlow()
	}

	private fun computeStaticValue() {
		if(isConditionAlwaysTrue)
			staticValue = positiveBranch.getValue()?.getComputedValue()
		if(isConditionAlwaysFalse)
			staticValue = negativeBranch?.getValue()?.getComputedValue()
	}

	private fun evaluateExecutionFlow() {
		isInterruptingExecutionBasedOnStructure = positiveBranch.isInterruptingExecutionBasedOnStructure
			&& negativeBranch?.isInterruptingExecutionBasedOnStructure == true
		isInterruptingExecutionBasedOnStaticEvaluation = (isConditionAlwaysTrue && positiveBranch.isInterruptingExecutionBasedOnStaticEvaluation) ||
			(isConditionAlwaysFalse && negativeBranch?.isInterruptingExecutionBasedOnStaticEvaluation == true) ||
			(positiveBranch.isInterruptingExecutionBasedOnStaticEvaluation && negativeBranch?.isInterruptingExecutionBasedOnStaticEvaluation == true)
	}

	override fun validate() {
		if(isPartOfExpression) {
			super.validate()
		} else {
			for(semanticModel in semanticModels)
				semanticModel.validate()
		}
		validateElseBranchExistence()
		validateValueExistence()
	}

	private fun validateElseBranchExistence() {
		if(!isPartOfExpression)
			return
		if(negativeBranch == null)
			context.addIssue(ExpressionMissesElse(source, EXPRESSION_TYPE))
	}

	private fun validateValueExistence() {
		if(!isPartOfExpression)
			return
		if(isInterruptingExecutionBasedOnStructure) {
			context.addIssue(ExpressionNeverReturns(source, EXPRESSION_TYPE))
			return
		}
		val lastStatementInPositiveBranch = positiveBranch.getLastStatement()
		if(!(lastStatementInPositiveBranch is Value || lastStatementInPositiveBranch?.isInterruptingExecutionBasedOnStructure == true))
			context.addIssue(BranchMissesValue(positiveBranch.source, EXPRESSION_TYPE))
		if(negativeBranch != null) {
			val lastStatementInNegativeBranch = negativeBranch.getLastStatement()
			if(!(lastStatementInNegativeBranch is Value || lastStatementInNegativeBranch?.isInterruptingExecutionBasedOnStructure == true))
				context.addIssue(BranchMissesValue(negativeBranch.source, EXPRESSION_TYPE))
		}
	}

	override fun compile(constructor: LlvmConstructor) {
		val function = constructor.getParentFunction()
		val condition = condition.getLlvmValue(constructor)
		val trueBlock = constructor.createBlock(function, "if_trueBlock")
		val falseBlock = constructor.createBlock(function, "if_falseBlock")
		val exitBlock = constructor.createBlock("if_exitBlock")
		constructor.buildJump(condition, trueBlock, falseBlock)
		constructor.select(trueBlock)
		positiveBranch.compile(constructor)
		if(!positiveBranch.isInterruptingExecutionBasedOnStructure)
			constructor.buildJump(exitBlock)
		constructor.select(falseBlock)
		negativeBranch?.compile(constructor)
		if(negativeBranch?.isInterruptingExecutionBasedOnStructure != true)
			constructor.buildJump(exitBlock)
		if(!isInterruptingExecutionBasedOnStructure) {
			constructor.addBlockToFunction(function, exitBlock)
			constructor.select(exitBlock)
		}
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val resultLlvmType = providedType?.getLlvmType(constructor)
		val result = constructor.buildStackAllocation(resultLlvmType, "if_resultVariable")
		val function = constructor.getParentFunction()
		val condition = condition.getLlvmValue(constructor)
		val trueBlock = constructor.createBlock(function, "if_trueBlock")
		val falseBlock = constructor.createBlock(function, "if_falseBlock")
		val exitBlock = constructor.createBlock("if_exitBlock")
		constructor.buildJump(condition, trueBlock, falseBlock)
		constructor.select(trueBlock)
		compileBranch(constructor, positiveBranch, result, exitBlock)
		constructor.select(falseBlock)
		val negativeBranch = negativeBranch ?: throw CompilerError(source, "If expression is missing a negative branch.")
		compileBranch(constructor, negativeBranch, result, exitBlock)
		if(!isInterruptingExecutionBasedOnStructure) {
			constructor.addBlockToFunction(function, exitBlock)
			constructor.select(exitBlock)
		}
		return constructor.buildLoad(resultLlvmType, result, "if_result")
	}

	private fun compileBranch(constructor: LlvmConstructor, branch: ErrorHandlingContext, result: LlvmValue, exitBlock: LlvmBlock) {
		if(branch.isInterruptingExecutionBasedOnStructure) {
			branch.compile(constructor)
			return
		}
		val statements = branch.mainBlock.statements
		val lastStatementIndex = statements.size - 1
		for((statementIndex, statement) in statements.withIndex()) {
			if(statementIndex == lastStatementIndex) {
				val value = statement as? Value ?: throw CompilerError(statement.source,
					"Last statement in if expression branch block doesn't provide a value.")
				constructor.buildStore(value.getLlvmValue(constructor), result)
			} else {
				statement.compile(constructor)
			}
		}
		constructor.buildJump(exitBlock)
	}
}
