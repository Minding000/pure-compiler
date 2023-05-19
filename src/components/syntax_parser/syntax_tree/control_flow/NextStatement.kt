package components.syntax_parser.syntax_tree.control_flow

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.Element
import components.tokenizer.Word
import components.semantic_analysis.semantic_model.control_flow.NextStatement as SemanticNextStatementModel

class NextStatement(word: Word): Element(word) {

	override fun toSemanticModel(scope: MutableScope): SemanticNextStatementModel {
		return SemanticNextStatementModel(this, scope)
	}

	override fun toString(): String {
		return "Next"
	}
}
