package parsing.ast.definitions

import parsing.ast.general.MetaElement
import parsing.tokenizer.Word

class GenericModifier(word: Word): MetaElement(word) {

	override fun toString(): String {
		return "GenericModifier { ${getValue()} }"
	}
}