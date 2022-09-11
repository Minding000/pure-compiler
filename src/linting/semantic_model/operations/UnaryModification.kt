package linting.semantic_model.operations

import linting.semantic_model.general.Unit
import parsing.syntax_tree.operations.UnaryModification

class UnaryModification(val source: UnaryModification, val target: Unit, val isIncrement: Boolean): Unit() {

	init {
		units.add(target)
	}
}