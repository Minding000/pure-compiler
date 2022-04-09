package linter.elements.access

import linter.elements.general.Unit
import parsing.ast.access.Index

class Index(val source: Index, val target: Unit, val indices: List<Unit>): Unit() {

	init {
		units.add(target)
		units.addAll(indices)
	}
}