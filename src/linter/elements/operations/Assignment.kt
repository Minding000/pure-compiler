package linter.elements.operations

import linter.elements.general.Unit
import parsing.ast.operations.Assignment

class Assignment(val source: Assignment, val targets: List<Unit>, val sourceExpression: Unit): Unit() {

	init {
		units.addAll(targets)
		units.add(sourceExpression)
	}
}