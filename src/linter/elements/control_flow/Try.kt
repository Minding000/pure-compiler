package linter.elements.control_flow

import linter.elements.general.Unit
import linter.elements.values.Value
import parsing.ast.control_flow.Try

class Try(val source: Try, val expression: Unit, val isOptional: Boolean): Value() {

	init {
		units.add(expression)
	}
}