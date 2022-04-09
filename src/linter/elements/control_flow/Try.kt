package linter.elements.control_flow

import linter.elements.general.Unit
import parsing.ast.control_flow.Try

class Try(val source: Try, val expression: Unit, val isOptional: Boolean): Unit() {

	init {
		units.add(expression)
	}
}