package linting.semantic_model.control_flow

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.scopes.Scope
import linting.semantic_model.values.BooleanLiteral
import linting.semantic_model.values.Value
import parsing.syntax_tree.control_flow.IfStatement

class IfStatement(override val source: IfStatement, val condition: Value, val trueBranch: Unit, val falseBranch: Unit?):
	Unit(source) {
	override var isInterruptingExecution = false
	var isConditionAlwaysTrue = false
	var isConditionAlwaysFalse = false

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
		if(isConditionAlwaysTrue)
			isInterruptingExecution = trueBranch.isInterruptingExecution
		if(isConditionAlwaysFalse && falseBranch != null)
			isInterruptingExecution = falseBranch.isInterruptingExecution
	}
}
