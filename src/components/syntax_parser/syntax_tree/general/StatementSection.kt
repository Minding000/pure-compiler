package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.HandleBlock as SemanticHandleBlockModel
import components.semantic_analysis.semantic_model.general.ErrorHandlingContext
import components.semantic_analysis.semantic_model.scopes.MutableScope
import java.util.*

class StatementSection(private val mainBlock: StatementBlock, val handleBlocks: List<HandleBlock> = LinkedList(),
					   private val alwaysBlock: StatementBlock? = null):
	Element(mainBlock.start, (alwaysBlock ?: handleBlocks.lastOrNull() ?: mainBlock).end) {

	override fun concretize(linter: Linter, scope: MutableScope): ErrorHandlingContext {
		val handleBlocks = LinkedList<SemanticHandleBlockModel>()
		for(handleBlock in this.handleBlocks)
			handleBlocks.add(handleBlock.concretize(linter, scope))
		return ErrorHandlingContext(this, mainBlock.concretize(linter, scope), handleBlocks,
			alwaysBlock?.concretize(linter, scope))
	}

	override fun toString(): String { //TODO clean this up
		return "StatementSection { $mainBlock${if(handleBlocks.isEmpty()) "" else " ${handleBlocks.joinToString(" ")}"}${if(alwaysBlock == null) "" else " $alwaysBlock"} }"
	}
}
