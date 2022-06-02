package linter.elements.operations

import linter.elements.general.Unit
import linter.elements.values.Value
import parsing.ast.operations.BinaryOperator

class BinaryOperator(val source: BinaryOperator, val left: Unit, val right: Unit, val operator: String): Value() {

	init {
		units.add(left)
		units.add(right)
	}
}