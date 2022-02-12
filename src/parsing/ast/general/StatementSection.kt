package parsing.ast.general

import parsing.ast.Element
import util.indent
import util.toLines

class StatementSection(val mainBlock: StatementBlock, val handleBlocks: List<HandleBlock>, val alwaysBlock: AlwaysBlock?):
	Element(mainBlock.start, (alwaysBlock ?: handleBlocks.lastOrNull() ?: mainBlock).end) {

	override fun toString(): String {
		return "StatementSection { $mainBlock${if(handleBlocks.isEmpty()) "" else " ${handleBlocks.joinToString(" ")}"}${if(alwaysBlock == null) "" else " $alwaysBlock"} }"
	}
}