package util

import linter.Linter
import linter.elements.general.Unit
import linter.scopes.Scope
import parsing.ast.general.Element
import java.util.*

fun List<Element>.concretize(linter: Linter, scope: Scope): List<Unit> {
	val units = LinkedList<Unit>()
	for(index in this)
		index.concretize(linter, scope, units)
	return units
}