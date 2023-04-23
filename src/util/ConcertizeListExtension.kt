package util

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.TypeElement
import components.syntax_parser.syntax_tree.general.ValueElement
import java.util.*

fun List<Element>.concretize(linter: Linter, scope: MutableScope): List<Unit> {
	val units = LinkedList<Unit>()
	for(element in this)
		element.concretize(linter, scope, units)
	return units
}

fun List<TypeElement>?.concretizeTypes(linter: Linter, scope: MutableScope): List<Type> {
	return this?.map { typeElement -> typeElement.concretize(linter, scope) } ?: listOf()
}

fun List<ValueElement>.concretizeValues(linter: Linter, scope: MutableScope): List<Value> {
	return map { valueElement -> valueElement.concretize(linter, scope) }
}
