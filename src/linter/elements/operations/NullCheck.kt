package linter.elements.operations

import linter.elements.values.Value
import parsing.ast.operations.NullCheck

class NullCheck(override val source: NullCheck, val value: Value): Value(source) {

	init {
		units.add(value)
	}
}