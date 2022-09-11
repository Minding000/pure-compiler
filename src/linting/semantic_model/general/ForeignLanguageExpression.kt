package linting.semantic_model.general

import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValue
import parsing.syntax_tree.general.ForeignLanguageExpression

class ForeignLanguageExpression(
	override val source: ForeignLanguageExpression, val foreignParser: VariableValue,
	val content: String): Value(source) {

	init {
		units.add(foreignParser)
	}
}