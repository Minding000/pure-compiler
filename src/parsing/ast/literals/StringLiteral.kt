package parsing.ast.literals

import linter.Linter
import linter.elements.values.StringLiteral
import linter.scopes.Scope
import parsing.ast.general.Element
import parsing.tokenizer.Word

class StringLiteral(word: Word): Element(word) {

	override fun concretize(linter: Linter, scope: Scope): StringLiteral {
		return StringLiteral(this, getValue())
	}

	override fun toString(): String {
		return "StringLiteral { ${getValue()} }"
	}
}