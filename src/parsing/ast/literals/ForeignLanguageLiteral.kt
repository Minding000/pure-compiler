package parsing.ast.literals

import parsing.ast.general.MetaElement
import parsing.tokenizer.Word

class ForeignLanguageLiteral(word: Word): MetaElement(word) {

	override fun toString(): String {
		return "ForeignLanguageLiteral { ${getValue()} }"
	}
}