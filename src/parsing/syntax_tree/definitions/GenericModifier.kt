package parsing.syntax_tree.definitions

import parsing.syntax_tree.general.MetaElement
import parsing.tokenizer.Word

class GenericModifier(word: Word): MetaElement(word) {

	override fun toString(): String {
		return "GenericModifier { ${getValue()} }"
	}
}