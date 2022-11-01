package components.parsing.syntax_tree.general

import components.linting.Linter
import components.linting.semantic_model.values.Value
import components.linting.semantic_model.scopes.MutableScope
import components.tokenizer.Word
import source_structure.Position

/**
 * Impacts code flow directly and returns a value
 */
abstract class ValueElement(start: Position, end: Position): Element(start, end) {

	constructor(word: Word): this(word.start, word.end)

	abstract override fun concretize(linter: Linter, scope: MutableScope): Value
}
