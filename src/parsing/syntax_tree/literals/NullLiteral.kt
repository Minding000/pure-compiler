package parsing.syntax_tree.literals

import linting.Linter
import linting.semantic_model.values.NullLiteral
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.ValueElement
import components.tokenizer.Word

class NullLiteral(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): NullLiteral {
		return NullLiteral(this)
	}

	override fun toString(): String {
		return "NullLiteral"
	}
}
