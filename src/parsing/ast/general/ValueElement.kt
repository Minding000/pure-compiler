package parsing.ast.general

import linter.Linter
import linter.elements.values.Value
import linter.scopes.MutableScope
import parsing.tokenizer.Word
import source_structure.Position

/**
 * Impacts code flow directly and returns a value
 */
abstract class ValueElement(start: Position, end: Position): Element(start, end) {

	constructor(word: Word): this(word.start, word.end)

	abstract override fun concretize(linter: Linter, scope: MutableScope): Value
}