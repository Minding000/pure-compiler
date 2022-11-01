package components.syntax_parser.syntax_tree.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.control_flow.NextStatement as SemanticNextStatementModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.Element
import components.tokenizer.Word

class NextStatement(word: Word): Element(word) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticNextStatementModel {
		return SemanticNextStatementModel(this)
	}

	override fun toString(): String {
		return "Next {  }"
	}
}
