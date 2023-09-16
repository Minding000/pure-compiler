package components.syntax_parser.syntax_tree.general

import components.semantic_model.scopes.MutableScope
import components.semantic_model.values.Value
import components.tokenizer.Word
import source_structure.Position

/**
 * Impacts code flow directly and returns a value
 */
abstract class ValueSyntaxTreeNode(start: Position, end: Position): SyntaxTreeNode(start, end) {

	constructor(word: Word): this(word.start, word.end)

	abstract override fun toSemanticModel(scope: MutableScope): Value
}
