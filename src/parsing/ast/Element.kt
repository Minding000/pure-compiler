package parsing.ast

import parsing.tokenizer.Word
import source_structure.Position
import source_structure.Section

abstract class Element(start: Position, end: Position): Section(start, end) {

	constructor(word: Word): this(word.start, word.end)
}