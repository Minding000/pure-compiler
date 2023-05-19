package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.tokenizer.Word
import source_structure.Position
import source_structure.Section

/**
 * Impacts semantic model directly and doesn't return
 */
abstract class Element(start: Position, end: Position): Section(start, end) {
	val context = start.line.file.module.project.context

	constructor(word: Word): this(word.start, word.end)

	open fun concretize(scope: MutableScope, units: MutableList<Unit>) {
		units.add(concretize(scope))
	}

	abstract fun concretize(scope: MutableScope): Unit
}
