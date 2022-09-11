package linting.semantic_model.control_flow

import linting.semantic_model.general.Unit
import parsing.syntax_tree.control_flow.Case

class Case(val source: Case, val condition: Unit, val result: Unit): Unit() {

	init {
		units.add(condition)
		units.add(result)
	}
}