package components.syntax_parser.syntax_tree.control_flow

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.Element
import components.tokenizer.Word
import components.semantic_analysis.semantic_model.control_flow.BreakStatement as SemanticBreakStatementModel

class BreakStatement(word: Word): Element(word) {

	override fun concretize(scope: MutableScope): SemanticBreakStatementModel {
		return SemanticBreakStatementModel(this, scope)
	}

	override fun toString(): String {
		return "Break"
	}
}
