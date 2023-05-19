package components.syntax_parser.syntax_tree.literals

import components.syntax_parser.syntax_tree.general.MetaSyntaxTreeNode
import components.tokenizer.Word

class ForeignLanguageLiteral(word: Word): MetaSyntaxTreeNode(word) {

	override fun toString(): String {
		return "ForeignLanguageLiteral { ${getValue()} }"
	}
}
