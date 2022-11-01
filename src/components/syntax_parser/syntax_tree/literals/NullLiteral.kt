package components.syntax_parser.syntax_tree.literals

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.values.NullLiteral as SemanticNullLiteralModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueElement
import components.tokenizer.Word

class NullLiteral(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticNullLiteralModel {
		return SemanticNullLiteralModel(this)
	}

	override fun toString(): String {
		return "NullLiteral"
	}
}
