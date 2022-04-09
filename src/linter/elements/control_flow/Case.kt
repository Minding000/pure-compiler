package linter.elements.control_flow

import linter.elements.general.Unit
import parsing.ast.control_flow.Case

class Case(val source: Case, val condition: Unit, val result: Unit): Unit() {

	init {
		units.add(condition)
		units.add(result)
	}
}