package linting.semantic_model.general

import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValue
import components.parsing.syntax_tree.general.ForeignLanguageExpression as ForeignLanguageExpressionSyntaxTree

class ForeignLanguageExpression(override val source: ForeignLanguageExpressionSyntaxTree,
								val foreignParser: VariableValue, val content: String): Value(source) {

	init {
		units.add(foreignParser)
	}
}
