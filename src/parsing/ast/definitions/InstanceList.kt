package parsing.ast.definitions

import parsing.ast.Element
import source_structure.Position
import util.indent
import util.toLines

class InstanceList(start: Position, val instances: List<Element>): Element(start, instances.last().end) {

	override fun toString(): String {
		return "InstanceList {${instances.toLines().indent()}\n}"
	}
}