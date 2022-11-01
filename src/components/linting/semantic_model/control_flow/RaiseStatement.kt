package components.linting.semantic_model.control_flow

import components.linting.semantic_model.general.Unit
import components.linting.semantic_model.values.Value
import components.syntax_parser.syntax_tree.control_flow.RaiseStatement as RaiseStatementSyntaxTree

class RaiseStatement(override val source: RaiseStatementSyntaxTree, val value: Value): Unit(source) {
	override val isInterruptingExecution = true

	init {
		units.add(value)
	}
}
