package linting.semantic_model.control_flow

import linting.semantic_model.general.Unit
import parsing.syntax_tree.control_flow.WhileGenerator

class WhileGenerator(val source: WhileGenerator, val condition: Unit, val isPostCondition: Boolean): Unit() {

	init {
		units.add(condition)
	}
}