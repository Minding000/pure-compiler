package parsing.syntax_tree.definitions

import parsing.syntax_tree.general.Element
import parsing.syntax_tree.general.MetaElement
import source_structure.Position
import util.indent
import util.toLines

class InstanceList(start: Position, private val instances: List<Element>): MetaElement(start, instances.last().end) {

	override fun toString(): String {
		return "InstanceList {${instances.toLines().indent()}\n}"
	}
}