package parsing.ast.literals

import linter.Linter
import linter.elements.values.NumberLiteral
import linter.scopes.MutableScope
import parsing.ast.general.ValueElement
import parsing.tokenizer.Word

class NumberLiteral(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): NumberLiteral {
		return NumberLiteral(this, getValue())
	}

	override fun toString(): String {
		return "NumberLiteral { ${getValue()} }"
	}
}