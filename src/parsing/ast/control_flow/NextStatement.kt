package parsing.ast.control_flow

import linter.Linter
import linter.elements.control_flow.NextStatement
import linter.scopes.Scope
import parsing.ast.general.Element
import parsing.tokenizer.Word

class NextStatement(word: Word): Element(word) {

	override fun concretize(linter: Linter, scope: Scope): NextStatement {
		return NextStatement(this)
	}

	override fun toString(): String {
		return "Next {  }"
	}
}