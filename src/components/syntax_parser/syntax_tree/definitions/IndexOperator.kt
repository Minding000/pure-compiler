package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.definitions.Parameter
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.MutableScope

class IndexOperator(private val parameterList: ParameterList): Operator(parameterList.start, parameterList.end) {

	fun concretizeGenerics(scope: MutableScope): List<TypeDefinition>? = parameterList.concretizeGenerics(scope)

	fun concretizeIndices(scope: MutableScope): List<Parameter> = parameterList.concretizeParameters(scope)

	override fun toString(): String {
		return "IndexOperator { $parameterList }"
	}
}
