package components.semantic_model.control_flow

import components.code_generation.llvm.models.control_flow.IfExpression
import components.semantic_model.context.VariableTracker
import components.semantic_model.general.ErrorHandlingContext
import components.semantic_model.scopes.Scope
import components.semantic_model.types.Type
import components.semantic_model.values.BooleanLiteral
import components.semantic_model.values.Value
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
		isInterruptingExecutionBasedOnStaticEvaluation =
			(isConditionAlwaysTrue && positiveBranch.isInterruptingExecutionBasedOnStaticEvaluation) ||
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

	override fun toUnit() = IfExpression(this, condition.toUnit(), positiveBranch.toUnit(), negativeBranch?.toUnit())
}
