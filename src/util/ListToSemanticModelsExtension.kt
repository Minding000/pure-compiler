package util

import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import java.util.*

fun List<SyntaxTreeNode>.toSemanticModels(scope: MutableScope): List<SemanticModel> {
	val semanticModels = LinkedList<SemanticModel>()
	for(element in this)
		element.toSemanticModel(scope, semanticModels)
	return semanticModels
}

fun List<TypeSyntaxTreeNode>?.toSemanticTypeModels(scope: MutableScope): List<Type> {
	return this?.map { typeElement -> typeElement.toSemanticModel(scope) } ?: emptyList()
}

fun List<ValueSyntaxTreeNode>.toSemanticValueModels(scope: MutableScope): List<Value> {
	return map { valueElement -> valueElement.toSemanticModel(scope) }
}
