package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.tokenizer.Word
import errors.internal.CompilerError
import source_structure.Position

/**
 * Doesn't impact semantic model directly
 */
abstract class MetaElement(start: Position, end: Position): Element(start, end) {

	constructor(word: Word): this(word.start, word.end)

	override fun concretize(linter: Linter, scope: MutableScope): Unit {
		throw CompilerError(this,
			"Tried to concretize meta element '${javaClass.canonicalName}' at ${getStartString()}:\n$this")
	}
}
