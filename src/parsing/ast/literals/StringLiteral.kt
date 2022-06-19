package parsing.ast.literals

import linter.Linter
import linter.elements.values.StringLiteral
import linter.scopes.MutableScope
import parsing.ast.general.ValueElement
import parsing.tokenizer.Word

class StringLiteral(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): StringLiteral {
		return StringLiteral(this, getValue())
	}

	override fun toString(): String {
		return "StringLiteral { ${getValue()} }"
	}
}