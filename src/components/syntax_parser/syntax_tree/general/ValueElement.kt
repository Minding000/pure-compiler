package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.values.Value
import components.tokenizer.Word
import source_structure.Position

/**
 * Impacts code flow directly and returns a value
 */
abstract class ValueElement(start: Position, end: Position): Element(start, end) {

	constructor(word: Word): this(word.start, word.end)

	abstract override fun concretize(scope: MutableScope): Value
}
