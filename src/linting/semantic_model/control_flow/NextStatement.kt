package linting.semantic_model.control_flow

import linting.semantic_model.general.Unit
import components.parsing.syntax_tree.control_flow.NextStatement

class NextStatement(override val source: NextStatement): Unit(source) {
	override val isInterruptingExecution = true
}
