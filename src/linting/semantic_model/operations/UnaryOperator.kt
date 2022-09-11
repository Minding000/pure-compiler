package linting.semantic_model.operations

import linting.semantic_model.values.Value
import parsing.syntax_tree.operations.UnaryOperator

class UnaryOperator(override val source: UnaryOperator, val value: Value, val operator: String): Value(source) {

	init {
		units.add(value)
	}
}