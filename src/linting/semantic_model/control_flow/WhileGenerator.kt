package linting.semantic_model.control_flow

import linting.semantic_model.general.Unit
import components.parsing.syntax_tree.control_flow.WhileGenerator

class WhileGenerator(override val source: WhileGenerator, val condition: Unit, val isPostCondition: Boolean):
	Unit(source) {
	override var isInterruptingExecution = false //TODO adjust this value based on condition and body

	init {
		units.add(condition)
	}
}
