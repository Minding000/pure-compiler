package components.semantic_analysis.semantic_model.general

import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import components.syntax_parser.syntax_tree.general.ForeignLanguageExpression as ForeignLanguageExpressionSyntaxTree

class ForeignLanguageExpression(override val source: ForeignLanguageExpressionSyntaxTree,
								val foreignParser: VariableValue, val content: String): Value(source) {

	init {
		addUnits(foreignParser)
	}
}
