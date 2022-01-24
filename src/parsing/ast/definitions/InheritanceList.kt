package parsing.ast.definitions

import parsing.ast.Element
import parsing.ast.literals.Type
import source_structure.Position
import util.indent
import util.toLines

class InheritanceList(start: Position, val parentTypes: List<Type>): Element(start, parentTypes.last().end) {

	override fun toString(): String {
		return "InheritanceList {${parentTypes.toLines().indent()}\n}"
	}
}