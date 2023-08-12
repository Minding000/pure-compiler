package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.definitions.Parameter
import components.semantic_analysis.semantic_model.definitions.TypeDeclaration
import components.semantic_analysis.semantic_model.scopes.MutableScope

class IndexOperator(private val parameterList: ParameterList): Operator(parameterList.start, parameterList.end) {

	fun getSemanticGenericParameterModels(scope: MutableScope): List<TypeDeclaration>? = parameterList.getSemanticGenericParameterModels(scope)

	fun getSemanticIndexParameterModels(scope: MutableScope): List<Parameter> = parameterList.getSemanticParameterModels(scope)

	override fun toString(): String {
		return "IndexOperator { $parameterList }"
	}
}
