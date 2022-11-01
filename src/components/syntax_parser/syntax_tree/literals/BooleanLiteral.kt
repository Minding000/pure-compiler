package components.syntax_parser.syntax_tree.literals

import components.linting.Linter
import components.linting.semantic_model.values.BooleanLiteral as SemanticBooleanLiteralModel
import components.linting.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueElement
import components.tokenizer.Word

class BooleanLiteral(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticBooleanLiteralModel {
		return SemanticBooleanLiteralModel(this, getValue() == "yes")
	}

	override fun toString(): String {
		return "BooleanLiteral { ${getValue()} }"
	}
}
