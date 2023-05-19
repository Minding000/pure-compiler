package components.syntax_parser.syntax_tree.literals

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.tokenizer.Word
import components.semantic_analysis.semantic_model.values.NullLiteral as SemanticNullLiteralModel

class NullLiteral(word: Word): ValueSyntaxTreeNode(word) {

	override fun toSemanticModel(scope: MutableScope): SemanticNullLiteralModel {
		return SemanticNullLiteralModel(this, scope)
	}

	override fun toString(): String {
		return "NullLiteral"
	}
}
