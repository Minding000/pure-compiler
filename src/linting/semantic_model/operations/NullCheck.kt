package linting.semantic_model.operations

import linting.semantic_model.values.Value
import parsing.syntax_tree.operations.NullCheck

class NullCheck(override val source: NullCheck, val value: Value): Value(source) {

	init {
		units.add(value)
	}
}