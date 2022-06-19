package linter.elements.operations

import linter.elements.values.Value
import parsing.ast.operations.UnaryOperator

class UnaryOperator(override val source: UnaryOperator, val value: Value, val operator: String): Value(source) {

	init {
		units.add(value)
	}
}