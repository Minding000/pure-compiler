package parsing.ast.literals

import parsing.ast.Element
import parsing.tokenizer.Word

class StringLiteral(word: Word): Element(word) {

	override fun toString(): String {
		return "StringLiteral { ${getValue()} }"
	}
}