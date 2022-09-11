package parsing.syntax_tree.literals

import linting.Linter
import linting.semantic_model.values.NumberLiteral
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.ValueElement
import parsing.tokenizer.Word

class NumberLiteral(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): NumberLiteral {
		return NumberLiteral(this, getValue())
	}

	override fun toString(): String {
		return "NumberLiteral { ${getValue()} }"
	}
}