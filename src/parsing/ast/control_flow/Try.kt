package parsing.ast.control_flow

import parsing.ast.Element
import source_structure.Position

class Try(val expression: Element, val isOptional: Boolean, start: Position): Element(start, expression.end) {

	override fun toString(): String {
		return "Try [ ${if(isOptional) "null" else "uncheck"} ] { $expression }"
	}
}