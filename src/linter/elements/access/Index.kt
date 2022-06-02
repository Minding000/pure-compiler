package linter.elements.access

import linter.elements.general.Unit
import linter.elements.values.Value
import parsing.ast.access.Index

class Index(val source: Index, val target: Unit, val indices: List<Unit>): Value() {

	init {
		units.add(target)
		units.addAll(indices)
	}
}