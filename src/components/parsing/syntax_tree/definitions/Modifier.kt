package components.parsing.syntax_tree.definitions

import components.parsing.syntax_tree.general.MetaElement
import components.tokenizer.Word

class Modifier(word: Word): MetaElement(word) {
	val type = word.type

	override fun toString(): String {
		return "Modifier { ${getValue()} }"
	}
}
