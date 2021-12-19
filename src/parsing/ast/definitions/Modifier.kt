package parsing.ast.definitions

import parsing.ast.Element
import parsing.tokenizer.Word

class Modifier(word: Word): Element(word) {

	override fun toString(): String {
		return "Modifier { ${getValue()} }"
	}
}