package components.parsing.syntax_tree.literals

import linting.Linter
import linting.semantic_model.values.BooleanLiteral as SemanticBooleanLiteralModel
import linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.ValueElement
import components.tokenizer.Word

class BooleanLiteral(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticBooleanLiteralModel {
		return SemanticBooleanLiteralModel(this, getValue() == "yes")
	}

	override fun toString(): String {
		return "BooleanLiteral { ${getValue()} }"
	}
}
