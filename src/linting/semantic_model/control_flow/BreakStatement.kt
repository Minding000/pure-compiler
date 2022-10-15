package linting.semantic_model.control_flow

import linting.semantic_model.general.Unit
import parsing.syntax_tree.control_flow.BreakStatement

class BreakStatement(override val source: BreakStatement): Unit(source) {
	override val isInterruptingExecution = true
}
