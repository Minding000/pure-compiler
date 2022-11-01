package parsing.syntax_tree.literals

import linting.Linter
import linting.semantic_model.values.StringLiteral
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.ValueElement
import components.tokenizer.Word

class StringLiteral(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): StringLiteral {
		return StringLiteral(this, getValue())
	}

	override fun toString(): String {
		return "StringLiteral { ${getValue()} }"
	}
}
