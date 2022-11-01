package components.parsing.syntax_tree.control_flow

import linting.Linter
import linting.semantic_model.control_flow.BreakStatement as SemanticBreakStatementModel
import linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.Element
import components.tokenizer.Word

class BreakStatement(word: Word): Element(word) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticBreakStatementModel {
		return SemanticBreakStatementModel(this)
	}

	override fun toString(): String {
		return "Break {  }"
	}
}
