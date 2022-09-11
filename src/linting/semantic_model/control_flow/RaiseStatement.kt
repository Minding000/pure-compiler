package linting.semantic_model.control_flow

import linting.semantic_model.general.Unit
import parsing.syntax_tree.control_flow.RaiseStatement

class RaiseStatement(val source: RaiseStatement, val value: Unit): Unit() {

	init {
		units.add(value)
	}
}