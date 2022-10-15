package linting.semantic_model.control_flow

import linting.semantic_model.general.Unit
import parsing.syntax_tree.control_flow.RaiseStatement

class RaiseStatement(override val source: RaiseStatement, val value: Unit): Unit(source) {
	override val isInterruptingExecution = true

	init {
		units.add(value)
	}
}
