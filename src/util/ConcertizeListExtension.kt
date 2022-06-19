package util

import linter.Linter
import linter.elements.general.Unit
import linter.elements.values.Value
import linter.scopes.MutableScope
import parsing.ast.general.Element
import parsing.ast.general.ValueElement
import java.util.*

fun List<Element>.concretize(linter: Linter, scope: MutableScope): List<Unit> {
	val units = LinkedList<Unit>()
	for(index in this)
		index.concretize(linter, scope, units)
	return units
}

fun List<ValueElement>.concretizeValues(linter: Linter, scope: MutableScope): List<Value> {
	val units = LinkedList<Value>()
	for(index in this)
		units.add(index.concretize(linter, scope))
	return units
}