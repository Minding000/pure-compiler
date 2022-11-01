package components.parsing.syntax_tree.general

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.scopes.MutableScope
import components.tokenizer.Word
import source_structure.Position
import source_structure.Section

/**
 * Impacts semantic model directly and doesn't return
 */
abstract class Element(start: Position, end: Position): Section(start, end) {

	constructor(word: Word): this(word.start, word.end)

	open fun concretize(linter: Linter, scope: MutableScope, units: MutableList<Unit>) {
		units.add(concretize(linter, scope))
	}

	abstract fun concretize(linter: Linter, scope: MutableScope): Unit
}
