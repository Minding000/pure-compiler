package parsing.ast.definitions

import parsing.ast.general.MetaElement
import parsing.tokenizer.Word

class Modifier(word: Word): MetaElement(word) {

	override fun toString(): String {
		return "Modifier { ${getValue()} }"
	}
}