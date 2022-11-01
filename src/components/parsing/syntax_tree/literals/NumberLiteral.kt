package components.parsing.syntax_tree.literals

import components.linting.Linter
import components.linting.semantic_model.values.NumberLiteral as SemanticNumberLiteralModel
import components.linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.ValueElement
import components.tokenizer.Word

class NumberLiteral(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticNumberLiteralModel {
		return SemanticNumberLiteralModel(this, getValue())
	}

	override fun toString(): String {
		return "NumberLiteral { ${getValue()} }"
	}
}
