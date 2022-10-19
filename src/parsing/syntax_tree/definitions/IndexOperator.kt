package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.definitions.Parameter
import linting.semantic_model.definitions.TypeDefinition
import linting.semantic_model.scopes.MutableScope

class IndexOperator(private val parameterList: ParameterList): Operator(parameterList.start, parameterList.end) {

	fun concretizeGenerics(linter: Linter, scope: MutableScope): List<TypeDefinition>? =
		parameterList.concretizeGenerics(linter, scope)

	fun concretizeIndices(linter: Linter, scope: MutableScope): List<Parameter> =
		parameterList.concretizeParameters(linter, scope)

	override fun toString(): String {
		return "IndexOperator { $parameterList }"
	}
}
