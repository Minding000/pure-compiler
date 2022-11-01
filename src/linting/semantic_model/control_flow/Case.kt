package linting.semantic_model.control_flow

import linting.semantic_model.general.Unit
import linting.semantic_model.values.Value
import components.parsing.syntax_tree.control_flow.Case as CaseSyntaxTree

class Case(override val source: CaseSyntaxTree, val condition: Value, val result: Unit): Unit(source) {

	init {
		units.add(condition)
		units.add(result)
	}
}
