package parsing.ast.literals

import linter.Linter
import linter.elements.values.VariableValue
import linter.scopes.Scope
import parsing.ast.general.Element
import parsing.tokenizer.Word

open class Identifier(word: Word): Element(word) {

	override fun concretize(linter: Linter, scope: Scope): VariableValue {
		return VariableValue(this)
	}

	override fun toString(): String {
		return "Identifier { ${getValue()} }"
	}
}