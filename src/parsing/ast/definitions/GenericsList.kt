package parsing.ast.definitions

import parsing.ast.general.MetaElement
import source_structure.Position
import util.indent
import util.toLines

class GenericsList(start: Position, val elements: List<GenericsListElement>, end: Position): MetaElement(start, end) {

	override fun toString(): String {
		return "GenericsList {${elements.toLines().indent()}\n}"
	}
}