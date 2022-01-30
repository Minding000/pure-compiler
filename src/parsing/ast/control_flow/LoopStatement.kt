package parsing.ast.control_flow

import parsing.ast.Element
import parsing.ast.general.StatementBlock
import source_structure.Position

class LoopStatement(start: Position, val generator: Element?, val body: StatementBlock): Element(start, body.end) {

	override fun toString(): String {
		return "Loop${if(generator == null) "" else " [ $generator ]"} { $body }"
	}
}