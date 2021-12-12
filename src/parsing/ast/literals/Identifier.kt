package parsing.ast.literals

import parsing.ast.Element
import parsing.tokenizer.Word

open class Identifier(word: Word): Element(word) {

	override fun toString(): String {
		return "Identifier { ${getValue()} }"
	}
}