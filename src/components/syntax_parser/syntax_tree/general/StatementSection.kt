package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.ErrorHandlingContext
import components.semantic_analysis.semantic_model.scopes.MutableScope
import java.util.*
import components.semantic_analysis.semantic_model.general.HandleBlock as SemanticHandleBlockModel

class StatementSection(private val mainBlock: StatementBlock, val handleBlocks: List<HandleBlock> = LinkedList(),
					   private val alwaysBlock: StatementBlock? = null):
	Element(mainBlock.start, (alwaysBlock ?: handleBlocks.lastOrNull() ?: mainBlock).end) {

	override fun concretize(linter: Linter, scope: MutableScope): ErrorHandlingContext {
		val handleBlocks = LinkedList<SemanticHandleBlockModel>()
		for(handleBlock in this.handleBlocks)
			handleBlocks.add(handleBlock.concretize(linter, scope))
		return ErrorHandlingContext(this, mainBlock.concretize(linter, scope), handleBlocks, alwaysBlock?.concretize(linter, scope))
	}

	override fun toString(): String {
		var stringRepresentation = "StatementSection { $mainBlock"
		if(handleBlocks.isNotEmpty())
			stringRepresentation += " ${handleBlocks.joinToString(" ")}"
		if(alwaysBlock != null)
			stringRepresentation += " $alwaysBlock"
		stringRepresentation += " }"
		return stringRepresentation
	}
}
