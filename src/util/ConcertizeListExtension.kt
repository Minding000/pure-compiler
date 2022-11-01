package util

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.values.Value
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.types.Type
import components.parsing.syntax_tree.general.Element
import components.parsing.syntax_tree.general.TypeElement
import components.parsing.syntax_tree.general.ValueElement
import java.util.*

fun List<Element>.concretize(linter: Linter, scope: MutableScope): List<Unit> {
	val units = LinkedList<Unit>()
	for(index in this)
		index.concretize(linter, scope, units)
	return units
}

fun List<TypeElement>?.concretizeTypes(linter: Linter, scope: MutableScope): List<Type> {
	if(this == null)
		return listOf()
	val units = LinkedList<Type>()
	for(index in this)
		units.add(index.concretize(linter, scope))
	return units
}

fun List<ValueElement>.concretizeValues(linter: Linter, scope: MutableScope): List<Value> {
	val units = LinkedList<Value>()
	for(index in this)
		units.add(index.concretize(linter, scope))
	return units
}
