package parsing.ast.general

import parsing.ast.Element
import source_structure.Position
import util.indent
import util.toLines

class AlwaysBlock(start: Position, val block: StatementBlock): Element(start, block.end) {

	override fun toString(): String {
		return "Always { $block }"
	}
}