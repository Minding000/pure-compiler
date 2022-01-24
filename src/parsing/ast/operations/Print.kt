package parsing.ast.operations

import source_structure.Position
import parsing.ast.Element
import util.indent
import util.toLines

// NOTE: This instruction is only for development purposes
class Print(start: Position, end: Position, val elements: List<Element>): Element(start, end) {

	override fun toString(): String {
		return "Print {${elements.toLines().indent()}\n}"
	}
}