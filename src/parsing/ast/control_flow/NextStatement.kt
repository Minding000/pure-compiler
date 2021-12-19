package parsing.ast.control_flow

import parsing.ast.Element
import parsing.tokenizer.Word

class NextStatement(word: Word): Element(word) {

	override fun toString(): String {
		return "Next {  }"
	}
}