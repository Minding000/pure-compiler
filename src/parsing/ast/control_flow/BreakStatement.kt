package parsing.ast.control_flow

import linter.Linter
import linter.elements.control_flow.BreakStatement
import linter.scopes.MutableScope
import parsing.ast.general.Element
import parsing.tokenizer.Word

class BreakStatement(word: Word): Element(word) {

	override fun concretize(linter: Linter, scope: MutableScope): BreakStatement {
		return BreakStatement(this)
	}

	override fun toString(): String {
		return "Break {  }"
	}
}