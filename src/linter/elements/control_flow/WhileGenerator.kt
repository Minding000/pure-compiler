package linter.elements.control_flow

import linter.elements.general.Unit
import parsing.ast.control_flow.WhileGenerator

class WhileGenerator(val source: WhileGenerator, val condition: Unit, val isPostCondition: Boolean): Unit() {

	init {
		units.add(condition)
	}
}