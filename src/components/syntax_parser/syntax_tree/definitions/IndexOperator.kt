package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.scopes.MutableScope

class IndexOperator(private val parameterList: ParameterList): Operator(parameterList.start, parameterList.end) {

	fun getSemanticGenericParameterModels(scope: MutableScope) = parameterList.getSemanticGenericParameterModels(scope)

	fun getSemanticIndexParameterModels(scope: MutableScope) = parameterList.getSemanticParameterModels(scope)

	override fun toString(): String {
		return "IndexOperator { $parameterList }"
	}
}
