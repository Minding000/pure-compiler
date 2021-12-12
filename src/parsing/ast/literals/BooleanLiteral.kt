package parsing.ast.literals

import parsing.ast.Element
import parsing.tokenizer.Word

class BooleanLiteral(word: Word): Element(word) {

	override fun toString(): String {
		return "BooleanLiteral { ${getValue()} }"
	}
}