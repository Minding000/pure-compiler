package linting.semantic_model.control_flow

import linting.semantic_model.general.Unit
import components.parsing.syntax_tree.control_flow.BreakStatement as BreakStatementSyntaxTree

class BreakStatement(override val source: BreakStatementSyntaxTree): Unit(source) {
	override val isInterruptingExecution = true
}
