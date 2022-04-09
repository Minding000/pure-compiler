package parsing.ast.literals

import linter.Linter
import linter.elements.values.NumberLiteral
import linter.scopes.Scope
import parsing.ast.general.Element
import parsing.tokenizer.Word

class NumberLiteral(word: Word): Element(word) {

	override fun concretize(linter: Linter, scope: Scope): NumberLiteral {
		return NumberLiteral(this, getValue())
	}

	override fun toString(): String {
		return "NumberLiteral { ${getValue()} }"
	}
}