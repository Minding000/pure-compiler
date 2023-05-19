package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.semantic_model.general.ErrorHandlingContext
import components.semantic_analysis.semantic_model.scopes.MutableScope
import java.util.*
import components.semantic_analysis.semantic_model.general.HandleBlock as SemanticHandleBlockModel

class StatementSection(private val mainBlock: StatementBlock, val handleBlocks: List<HandleBlock> = LinkedList(),
					   private val alwaysBlock: StatementBlock? = null):
	SyntaxTreeNode(mainBlock.start, (alwaysBlock ?: handleBlocks.lastOrNull() ?: mainBlock).end) {

	override fun toSemanticModel(scope: MutableScope): ErrorHandlingContext {
		val handleBlocks = LinkedList<SemanticHandleBlockModel>()
		for(handleBlock in this.handleBlocks)
			handleBlocks.add(handleBlock.toSemanticModel(scope))
		return ErrorHandlingContext(this, scope, mainBlock.toSemanticModel(scope), handleBlocks, alwaysBlock?.toSemanticModel(scope))
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
