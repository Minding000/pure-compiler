package linter.elements.operations

import linter.elements.values.Value
import parsing.ast.operations.NullCheck

class NullCheck(val source: NullCheck, val value: Value): Value() {

	init {
		units.add(value)
	}
}