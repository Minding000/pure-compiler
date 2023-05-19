package components.syntax_parser.syntax_tree.literals

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueElement
import components.tokenizer.Word
import components.semantic_analysis.semantic_model.values.StringLiteral as SemanticStringLiteralModel

class StringLiteral(word: Word): ValueElement(word) {

	override fun concretize(scope: MutableScope): SemanticStringLiteralModel {
		return SemanticStringLiteralModel(this, scope, getValue())
	}

	override fun toString(): String {
		return "StringLiteral { ${getValue()} }"
	}
}
