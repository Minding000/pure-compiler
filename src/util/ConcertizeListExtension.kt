package util

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.values.Value
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.general.ValueElement
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