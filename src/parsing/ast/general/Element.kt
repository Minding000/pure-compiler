package parsing.ast.general

import linter.Linter
import linter.elements.general.Unit
import linter.scopes.Scope
import parsing.tokenizer.Word
import source_structure.Position
import source_structure.Section

/**
 * Impacts code flow directly, but doesn't return
 */
abstract class Element(start: Position, end: Position): Section(start, end) {

	constructor(word: Word): this(word.start, word.end)

	abstract fun concretize(linter: Linter, scope: Scope): Unit

	open fun concretize(linter: Linter, scope: Scope, units: MutableList<Unit>) {
		units.add(concretize(linter, scope))
	}
}