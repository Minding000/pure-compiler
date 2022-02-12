package parsing.ast.control_flow

import parsing.ast.Element
import source_structure.Position

class RaiseStatement(val value: Element, start: Position): Element(start, value.end) {

	override fun toString(): String {
		return "Raise { $value }"
	}
}