package components.linting.semantic_model.control_flow

import components.linting.Linter
import components.linting.semantic_model.general.Unit
import components.linting.semantic_model.scopes.Scope
import components.linting.semantic_model.values.BooleanLiteral
import components.linting.semantic_model.values.Value
import components.parsing.syntax_tree.control_flow.IfStatement as IfStatementSyntaxTree

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
