package parsing.syntax_tree.definitions

import parsing.syntax_tree.general.MetaElement
import parsing.tokenizer.Word

class Modifier(word: Word): MetaElement(word) {
	val type = word.type

	override fun toString(): String {
		return "Modifier { ${getValue()} }"
	}
}