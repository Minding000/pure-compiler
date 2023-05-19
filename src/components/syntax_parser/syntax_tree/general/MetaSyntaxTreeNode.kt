package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.tokenizer.Word
import errors.internal.CompilerError
import source_structure.Position

/**
 * Doesn't impact semantic model directly
 */
abstract class MetaSyntaxTreeNode(start: Position, end: Position): SyntaxTreeNode(start, end) {

	constructor(word: Word): this(word.start, word.end)

	override fun toSemanticModel(scope: MutableScope): SemanticModel {
		throw CompilerError(this,
			"Tried to create semantic model of meta element '${javaClass.canonicalName}' at ${getStartString()}:\n$this")
	}
}
