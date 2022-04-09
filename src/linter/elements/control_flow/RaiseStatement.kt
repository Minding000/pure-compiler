package linter.elements.control_flow

import linter.elements.general.Unit
import parsing.ast.control_flow.RaiseStatement

class RaiseStatement(val source: RaiseStatement, val value: Unit): Unit() {

	init {
		units.add(value)
	}
}