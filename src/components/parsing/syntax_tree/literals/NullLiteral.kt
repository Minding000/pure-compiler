package components.parsing.syntax_tree.literals

import linting.Linter
import linting.semantic_model.values.NullLiteral as SemanticNullLiteralModel
import linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.ValueElement
import components.tokenizer.Word

class NullLiteral(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticNullLiteralModel {
		return SemanticNullLiteralModel(this)
	}

	override fun toString(): String {
		return "NullLiteral"
	}
}
