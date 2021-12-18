package parsing.ast.definitions

import parsing.ast.Element
import parsing.tokenizer.Word

class TypeType(word: Word): Element(word) {

	override fun toString(): String {
		return "TypeType { ${getValue()} }"
	}
}