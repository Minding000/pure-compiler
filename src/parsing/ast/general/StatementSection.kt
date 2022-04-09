package parsing.ast.general

import linter.Linter
import linter.elements.general.ErrorHandlingContext
import linter.scopes.Scope
import parsing.ast.general.StatementBlock
import java.util.*

class StatementSection(val mainBlock: StatementBlock, val handleBlocks: List<HandleBlock>, val alwaysBlock: StatementBlock?):
	Element(mainBlock.start, (alwaysBlock ?: handleBlocks.lastOrNull() ?: mainBlock).end) {

	override fun concretize(linter: Linter, scope: Scope): ErrorHandlingContext {
		val handleBlocks = LinkedList<linter.elements.general.HandleBlock>()
		for(handleBlock in this.handleBlocks)
			handleBlocks.add(handleBlock.concretize(linter, scope))
		return ErrorHandlingContext(this, mainBlock.concretize(linter, scope), handleBlocks, alwaysBlock?.concretize(linter, scope))
	}

	override fun toString(): String {
		return "StatementSection { $mainBlock${if(handleBlocks.isEmpty()) "" else " ${handleBlocks.joinToString(" ")}"}${if(alwaysBlock == null) "" else " $alwaysBlock"} }"
	}
}