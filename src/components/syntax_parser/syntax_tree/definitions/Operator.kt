package components.syntax_parser.syntax_tree.definitions

import components.syntax_parser.syntax_tree.general.MetaElement
import components.tokenizer.Word
import source_structure.Position

open class Operator(start: Position, end: Position): MetaElement(start, end) {

	constructor(word: Word): this(word.start, word.end)

	override fun toString(): String {
		return "Operator { ${getValue()} }"
	}
}
