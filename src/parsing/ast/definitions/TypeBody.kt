package parsing.ast.definitions

import parsing.ast.general.Element
import parsing.ast.general.MetaElement
import source_structure.Position
import util.indent
import util.toLines

class TypeBody(start: Position, end: Position, val members: List<Element>): MetaElement(start, end) {

	override fun toString(): String {
		return "TypeBody {${members.toLines().indent()}\n}"
	}
}