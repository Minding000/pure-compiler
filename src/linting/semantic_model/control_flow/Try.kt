package linting.semantic_model.control_flow

import linting.semantic_model.general.Unit
import linting.semantic_model.values.Value
import parsing.syntax_tree.control_flow.Try

class Try(override val source: Try, val expression: Unit, val isOptional: Boolean): Value(source) {

	init {
		units.add(expression)
	}
}