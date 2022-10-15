package linting.semantic_model.operations

import linting.semantic_model.general.Unit
import parsing.syntax_tree.operations.UnaryModification

class UnaryModification(override val source: UnaryModification, val target: Unit, val isIncrement: Boolean):
	Unit(source) {

	init {
		units.add(target)
	}
}
