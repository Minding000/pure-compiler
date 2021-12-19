package parsing.ast.control_flow

import parsing.ast.Element
import parsing.tokenizer.Word

class BreakStatement(word: Word): Element(word) {

	override fun toString(): String {
		return "Break {  }"
	}
}