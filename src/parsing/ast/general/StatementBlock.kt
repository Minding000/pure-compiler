package parsing.ast.general

import parsing.ast.Element
import source_structure.Position
import util.indent
import util.toLines

class StatementBlock(start: Position, end: Position, val statements: List<Element>): Element(start, end) {

	override fun toString(): String {
		return "StatementBlock {${statements.toLines().indent()}\n}"
	}
}