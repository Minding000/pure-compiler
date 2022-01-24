package parsing.ast.definitions

import parsing.ast.Element
import parsing.tokenizer.Word

class GenericModifier(word: Word): Element(word) {

	override fun toString(): String {
		return "GenericModifier { ${getValue()} }"
	}
}