package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.BooleanLiteral
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.control_flow.IfStatement as IfStatementSyntaxTree

class IfStatement(override val source: IfStatementSyntaxTree, scope: Scope, val condition: Value, val positiveBranch: Unit,
				  val negativeBranch: Unit?): Unit(source, scope) {
	override var isInterruptingExecution = false
	private var isConditionAlwaysTrue = false
	private var isConditionAlwaysFalse = false

	init {
		addUnits(condition, positiveBranch, negativeBranch)
	}

	override fun linkValues(linter: Linter) {
		super.linkValues(linter)
		(condition.staticValue as? BooleanLiteral)?.value.let { staticResult ->
			isConditionAlwaysTrue = staticResult == true
			isConditionAlwaysFalse = staticResult == false
		}
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		condition.analyseDataFlow(linter, tracker)
		val conditionState = tracker.currentState.copy()
		positiveBranch.analyseDataFlow(linter, tracker)
		val positiveBranchState = tracker.currentState.copy()
		if(negativeBranch == null) {
			tracker.addVariableStates(conditionState)
		} else {
			tracker.setVariableStates(conditionState)
			negativeBranch.analyseDataFlow(linter, tracker)
			tracker.addVariableStates(positiveBranchState)
		}
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		isInterruptingExecution = (isConditionAlwaysTrue && positiveBranch.isInterruptingExecution) ||
			(isConditionAlwaysFalse && negativeBranch?.isInterruptingExecution == true) ||
			(positiveBranch.isInterruptingExecution && negativeBranch?.isInterruptingExecution == true)
	}
}
