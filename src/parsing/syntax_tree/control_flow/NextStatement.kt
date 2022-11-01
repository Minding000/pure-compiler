package parsing.syntax_tree.control_flow

import linting.Linter
import linting.semantic_model.control_flow.NextStatement
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.Element
import components.tokenizer.Word

class NextStatement(word: Word): Element(word) {

	override fun concretize(linter: Linter, scope: MutableScope): NextStatement {
		return NextStatement(this)
	}

	override fun toString(): String {
		return "Next {  }"
	}
}
