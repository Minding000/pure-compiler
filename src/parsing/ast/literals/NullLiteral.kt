package parsing.ast.literals

import linter.Linter
import linter.elements.values.NullLiteral
import linter.scopes.MutableScope
import parsing.ast.general.ValueElement
import parsing.tokenizer.Word

class NullLiteral(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): NullLiteral {
		return NullLiteral(this)
	}

	override fun toString(): String {
		return "NullLiteral"
	}
}