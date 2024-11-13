package components.semantic_model.general

import components.semantic_model.scopes.Scope
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue
import components.code_generation.llvm.models.values.Value as ValueUnit
import components.syntax_parser.syntax_tree.general.ForeignLanguageExpression as ForeignLanguageExpressionSyntaxTree

class ForeignLanguageExpression(override val source: ForeignLanguageExpressionSyntaxTree, scope: Scope, val foreignParser: VariableValue,
								val content: String): Value(source, scope) {

	init {
		addSemanticModels(foreignParser)
	}

	override fun toUnit(): ValueUnit {
		TODO("Not yet implemented")
	}
}
