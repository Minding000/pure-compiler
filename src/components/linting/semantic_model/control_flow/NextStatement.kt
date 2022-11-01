package components.linting.semantic_model.control_flow

import components.linting.semantic_model.general.Unit
import components.syntax_parser.syntax_tree.control_flow.NextStatement as NextStatementSyntaxTree

class NextStatement(override val source: NextStatementSyntaxTree): Unit(source) {
	override val isInterruptingExecution = true
}
