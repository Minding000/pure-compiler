package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.semantic_model.general.Unit
import components.syntax_parser.syntax_tree.control_flow.BreakStatement as BreakStatementSyntaxTree

class BreakStatement(override val source: BreakStatementSyntaxTree): Unit(source) {
	override val isInterruptingExecution = true
}
