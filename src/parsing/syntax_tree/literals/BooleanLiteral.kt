package parsing.syntax_tree.literals

import linting.Linter
import linting.semantic_model.values.BooleanLiteral
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.ValueElement
import parsing.tokenizer.Word

class BooleanLiteral(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): BooleanLiteral {
		return BooleanLiteral(this, getValue() == "yes")
	}

	override fun toString(): String {
		return "BooleanLiteral { ${getValue()} }"
	}
}