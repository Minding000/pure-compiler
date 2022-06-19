package parsing.ast.general

import errors.internal.CompilerError
import linter.Linter
import linter.elements.general.Unit
import linter.scopes.MutableScope
import parsing.tokenizer.Word
import source_structure.Position

/**
 * Doesn't impact code flow directly
 */
abstract class MetaElement(start: Position, end: Position): Element(start, end) {

	constructor(word: Word): this(word.start, word.end)

	override fun concretize(linter: Linter, scope: MutableScope): Unit {
		throw CompilerError("Tried to concretize meta element at ${getStartString()}:\n$this")
	}
}