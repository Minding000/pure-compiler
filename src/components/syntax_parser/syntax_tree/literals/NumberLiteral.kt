package components.syntax_parser.syntax_tree.literals

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueElement
import components.tokenizer.Word
import java.math.BigDecimal
import components.semantic_analysis.semantic_model.values.NumberLiteral as SemanticNumberLiteralModel

class NumberLiteral(word: Word): ValueElement(word) {

	override fun concretize(scope: MutableScope): SemanticNumberLiteralModel {
		val value = BigDecimal(getValue().replace("_", ""))
		return SemanticNumberLiteralModel(this, scope, value)
	}

	override fun toString(): String {
		return "NumberLiteral { ${getValue()} }"
	}
}
