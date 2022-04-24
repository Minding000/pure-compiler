package linter.elements.operations

import linter.elements.general.Unit
import parsing.ast.operations.BinaryOperator

class BinaryOperator(val source: BinaryOperator, val left: Unit, val right: Unit, val operator: String): Unit() {

	init {
		units.add(left)
		units.add(right)
	}
}