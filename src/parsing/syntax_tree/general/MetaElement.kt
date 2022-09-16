package parsing.syntax_tree.general

import errors.internal.CompilerError
import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.scopes.MutableScope
import parsing.tokenizer.Word
import source_structure.Position

/**
 * Doesn't impact semantic model directly
 */
abstract class MetaElement(start: Position, end: Position): Element(start, end) {

	constructor(word: Word): this(word.start, word.end)

	override fun concretize(linter: Linter, scope: MutableScope): Unit {
		throw CompilerError("Tried to concretize meta element at ${getStartString()}:\n$this")
	}
}