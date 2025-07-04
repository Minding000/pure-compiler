package components.syntax_parser.syntax_tree.general

import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.MutableScope
import components.tokenizer.Word
import source_structure.Position
import source_structure.Section

/**
 * Impacts semantic model directly and doesn't return
 */
abstract class SyntaxTreeNode(start: Position, end: Position): Section(start, end) {
	val context = start.line.file.module.project.context

	constructor(word: Word): this(word.start, word.end)

	open fun toSemanticModel(scope: MutableScope, semanticModels: MutableList<SemanticModel>) {
		semanticModels.add(toSemanticModel(scope))
	}

	abstract fun toSemanticModel(scope: MutableScope): SemanticModel
}
