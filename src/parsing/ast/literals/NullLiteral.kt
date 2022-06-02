package parsing.ast.literals

import linter.Linter
import linter.elements.values.NullLiteral
import linter.scopes.Scope
import parsing.ast.general.ValueElement
import parsing.tokenizer.Word

class NullLiteral(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: Scope): NullLiteral {
		return NullLiteral(this)
	}

	override fun toString(): String {
		return "NullLiteral"
	}
}