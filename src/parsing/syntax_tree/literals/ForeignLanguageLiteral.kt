package parsing.syntax_tree.literals

import parsing.syntax_tree.general.MetaElement
import parsing.tokenizer.Word

class ForeignLanguageLiteral(word: Word): MetaElement(word) {

	override fun toString(): String {
		return "ForeignLanguageLiteral { ${getValue()} }"
	}
}