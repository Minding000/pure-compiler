package parsing.ast.definitions

import parsing.ast.Element
import parsing.ast.literals.Identifier
import source_structure.Position
import util.indent
import util.toLines

class GenericsList(val identifiers: List<Identifier>, start: Position, end: Position): Element(start, end) {

	override fun toString(): String {
		return "GenericsList {${identifiers.toLines().indent()}\n}"
	}
}