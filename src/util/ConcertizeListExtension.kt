package util

import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.TypeElement
import components.syntax_parser.syntax_tree.general.ValueElement
import java.util.*

fun List<Element>.toSemanticModels(scope: MutableScope): List<Unit> {
	val units = LinkedList<Unit>()
	for(element in this)
		element.toSemanticModel(scope, units)
	return units
}

fun List<TypeElement>?.toSemanticTypeModels(scope: MutableScope): List<Type> {
	return this?.map { typeElement -> typeElement.toSemanticModel(scope) } ?: listOf()
}

fun List<ValueElement>.toSemanticValueModels(scope: MutableScope): List<Value> {
	return map { valueElement -> valueElement.toSemanticModel(scope) }
}
