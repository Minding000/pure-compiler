package components.semantic_analysis.semantic_model.general

import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import components.syntax_parser.syntax_tree.general.ForeignLanguageExpression as ForeignLanguageExpressionSyntaxTree

class ForeignLanguageExpression(override val source: ForeignLanguageExpressionSyntaxTree, scope: Scope, val foreignParser: VariableValue,
								val content: String): Value(source, scope) {

	init {
		addUnits(foreignParser)
	}
}
