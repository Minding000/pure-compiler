package linter.elements.operators

import linter.elements.general.Unit
import parsing.ast.operations.UnaryModification

class UnaryModification(val source: UnaryModification, val target: Unit, val isIncrement: Boolean): Unit() {

	init {
		units.add(target)
	}
}