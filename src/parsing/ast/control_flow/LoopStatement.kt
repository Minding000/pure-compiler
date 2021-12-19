package parsing.ast.control_flow

import parsing.ast.Element
import source_structure.Position

class LoopStatement(start: Position, val body: StatementBlock): Element(start, body.end) {

	override fun toString(): String {
		return "Loop { $body }"
	}
}