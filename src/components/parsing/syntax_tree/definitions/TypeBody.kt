package components.parsing.syntax_tree.definitions

import components.parsing.syntax_tree.general.Element
import components.parsing.syntax_tree.general.MetaElement
import source_structure.Position
import util.indent
import util.toLines

class TypeBody(start: Position, end: Position, val members: List<Element>): MetaElement(start, end) {

	override fun toString(): String {
		return "TypeBody {${members.toLines().indent()}\n}"
	}
}
