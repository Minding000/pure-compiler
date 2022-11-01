package linting.semantic_model.control_flow

import linting.semantic_model.general.Unit
import components.parsing.syntax_tree.control_flow.NextStatement as NextStatementSyntaxTree

class NextStatement(override val source: NextStatementSyntaxTree): Unit(source) {
	override val isInterruptingExecution = true
}
