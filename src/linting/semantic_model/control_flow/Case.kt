package linting.semantic_model.control_flow

import linting.semantic_model.general.Unit
import linting.semantic_model.values.Value
import parsing.syntax_tree.control_flow.Case

class Case(override val source: Case, val condition: Value, val result: Unit): Unit(source) {

	init {
		units.add(condition)
		units.add(result)
	}
}
