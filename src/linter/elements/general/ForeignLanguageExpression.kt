package linter.elements.general

import linter.elements.values.Value
import linter.elements.values.VariableValue
import parsing.ast.general.ForeignLanguageExpression

class ForeignLanguageExpression(val source: ForeignLanguageExpression, val foreignParser: VariableValue,
								val content: String): Value() {

	init {
		units.add(foreignParser)
	}
}