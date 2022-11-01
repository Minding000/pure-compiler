package components.parsing.syntax_tree.literals

import components.parsing.syntax_tree.general.MetaElement
import components.tokenizer.Word

class ForeignLanguageLiteral(word: Word): MetaElement(word) {

	override fun toString(): String {
		return "ForeignLanguageLiteral { ${getValue()} }"
	}
}
