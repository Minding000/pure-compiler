package parsing.ast.literals

import parsing.ast.Element
import parsing.tokenizer.Word

class NumberLiteral(word: Word): Element(word) {

	override fun toString(): String {
		return "NumberLiteral { ${getValue()} }"
	}
}