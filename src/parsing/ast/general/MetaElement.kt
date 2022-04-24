package parsing.ast.general

import errors.internal.CompilerError
import linter.Linter
import linter.elements.general.Unit
import linter.scopes.Scope
import parsing.tokenizer.Word
import source_structure.Position

abstract class MetaElement(start: Position, end: Position): Element(start, end) {

	constructor(word: Word): this(word.start, word.end)

	override fun concretize(linter: Linter, scope: Scope): Unit {
		throw CompilerError("Tried to concretize meta element at ${getStartString()}:\n$this")
	}
}