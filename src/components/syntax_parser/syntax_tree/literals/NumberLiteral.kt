package components.syntax_parser.syntax_tree.literals

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.values.NumberLiteral as SemanticNumberLiteralModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueElement
import components.tokenizer.Word
import java.math.BigDecimal

class NumberLiteral(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticNumberLiteralModel {
		val value = BigDecimal(getValue().replace("_", ""))
		return SemanticNumberLiteralModel(this, value)
	}

	override fun toString(): String {
		return "NumberLiteral { ${getValue()} }"
	}
}
