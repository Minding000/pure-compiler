package components.syntax_parser.syntax_tree.literals

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.values.NumberLiteral as SemanticNumberLiteralModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueElement
import components.tokenizer.Word

class NumberLiteral(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticNumberLiteralModel {
		return SemanticNumberLiteralModel(this, getValue())
	}

	override fun toString(): String {
		return "NumberLiteral { ${getValue()} }"
	}
}
