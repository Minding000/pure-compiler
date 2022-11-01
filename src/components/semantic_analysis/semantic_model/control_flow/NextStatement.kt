package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.semantic_model.general.Unit
import components.syntax_parser.syntax_tree.control_flow.NextStatement as NextStatementSyntaxTree

class NextStatement(override val source: NextStatementSyntaxTree): Unit(source) {
	override val isInterruptingExecution = true
}
