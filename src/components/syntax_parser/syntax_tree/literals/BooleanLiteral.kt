package components.syntax_parser.syntax_tree.literals

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.tokenizer.Word
import components.semantic_analysis.semantic_model.values.BooleanLiteral as SemanticBooleanLiteralModel

class BooleanLiteral(word: Word): ValueSyntaxTreeNode(word) {

	override fun toSemanticModel(scope: MutableScope): SemanticBooleanLiteralModel {
		return SemanticBooleanLiteralModel(this, scope, getValue() == "yes")
	}

	override fun toString(): String {
		return "BooleanLiteral { ${getValue()} }"
	}
}
