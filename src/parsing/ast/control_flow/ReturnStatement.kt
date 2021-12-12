package parsing.ast.control_flow

import parsing.ast.Element
import source_structure.Position

class ReturnStatement(val value: Element?, start: Position, end: Position): Element(start, end) {

	override fun toString(): String {
		return "Return { ${value ?: ""} }"
	}
}