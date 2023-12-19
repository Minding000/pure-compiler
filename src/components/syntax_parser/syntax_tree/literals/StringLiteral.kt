package components.syntax_parser.syntax_tree.literals

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.tokenizer.Word
import components.semantic_model.values.StringLiteral as SemanticStringLiteralModel

class StringLiteral(word: Word, val content: String): ValueSyntaxTreeNode(word) {

	override fun toSemanticModel(scope: MutableScope): SemanticStringLiteralModel {
		return SemanticStringLiteralModel(this, scope, content)
	}

	override fun toString(): String {
		return "StringLiteral { \"$content\" }"
	}
}
