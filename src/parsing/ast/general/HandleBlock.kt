package parsing.ast.general

import parsing.ast.Element
import parsing.ast.literals.Identifier
import parsing.ast.literals.Type
import source_structure.Position
import util.indent
import util.toLines

class HandleBlock(start: Position, val type: Type, val identifier: Identifier?, val block: StatementBlock): Element(start, block.end) {

	override fun toString(): String {
		return "Handle [ $type${if(identifier == null) "" else " $identifier"} ] { $block }"
	}
}