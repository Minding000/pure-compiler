package parsing.ast.definitions

import parsing.ast.general.MetaElement
import parsing.tokenizer.Word
import source_structure.Position

open class Operator(start: Position, end: Position): MetaElement(start, end) {

	constructor(word: Word): this(word.start, word.end)

	override fun toString(): String {
		return "Operator { ${getValue()} }"
	}
}