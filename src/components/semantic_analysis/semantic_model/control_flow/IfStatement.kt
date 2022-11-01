package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.BooleanLiteral
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.control_flow.IfStatement as IfStatementSyntaxTree

class IfStatement(override val source: IfStatementSyntaxTree, val condition: Value, val trueBranch: Unit,
				  val falseBranch: Unit?): Unit(source) {
	override var isInterruptingExecution = false
	private var isConditionAlwaysTrue = false
	private var isConditionAlwaysFalse = false

	init {
		units.add(condition)
		units.add(trueBranch)
		if(falseBranch != null)
			units.add(falseBranch)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		(condition.staticValue as? BooleanLiteral)?.value.let { staticResult ->
			isConditionAlwaysTrue = staticResult == true
			isConditionAlwaysFalse = staticResult == false
		}
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		isInterruptingExecution = (isConditionAlwaysTrue && trueBranch.isInterruptingExecution) ||
			(isConditionAlwaysFalse && falseBranch?.isInterruptingExecution == true) ||
			(trueBranch.isInterruptingExecution && falseBranch?.isInterruptingExecution == true)
	}
}
