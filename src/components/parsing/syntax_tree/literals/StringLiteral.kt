package components.parsing.syntax_tree.literals

import components.linting.Linter
import components.linting.semantic_model.values.StringLiteral as SemanticStringLiteralModel
import components.linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.ValueElement
import components.tokenizer.Word

class StringLiteral(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticStringLiteralModel {
		return SemanticStringLiteralModel(this, getValue())
	}

	override fun toString(): String {
		return "StringLiteral { ${getValue()} }"
	}
}
