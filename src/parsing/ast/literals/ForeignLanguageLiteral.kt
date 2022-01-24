package parsing.ast.literals

import parsing.ast.Element
import parsing.tokenizer.Word

class ForeignLanguageLiteral(word: Word): Element(word) {

	override fun toString(): String {
		return "ForeignLanguageLiteral { ${getValue()} }"
	}
}