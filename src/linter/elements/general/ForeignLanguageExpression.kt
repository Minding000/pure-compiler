package linter.elements.general

import linter.elements.values.Value
import linter.elements.values.VariableValue
import parsing.ast.general.ForeignLanguageExpression

class ForeignLanguageExpression(
	override val source: ForeignLanguageExpression, val foreignParser: VariableValue,
	val content: String): Value(source) {

	init {
		units.add(foreignParser)
	}
}