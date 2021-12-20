package parsing.ast.definitions

import parsing.ast.Element
import parsing.tokenizer.Word
import source_structure.Position

open class Operator(start: Position, end: Position): Element(start, end) {

	constructor(word: Word): this(word.start, word.end)

	override fun toString(): String {
		return "Operator { ${getValue()} }"
	}
}