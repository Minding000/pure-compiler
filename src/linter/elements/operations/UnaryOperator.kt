package linter.elements.operations

import linter.elements.values.Value
import parsing.ast.operations.UnaryOperator

class UnaryOperator(val source: UnaryOperator, val value: Value, val operator: String): Value() {

	init {
		units.add(value)
	}
}