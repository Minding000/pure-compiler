package linting.semantic_model.control_flow

import linting.semantic_model.general.Unit
import parsing.syntax_tree.control_flow.WhileGenerator

class WhileGenerator(override val source: WhileGenerator, val condition: Unit, val isPostCondition: Boolean):
	Unit(source) {

	init {
		units.add(condition)
	}
}
