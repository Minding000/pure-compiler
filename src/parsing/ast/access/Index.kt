package parsing.ast.access

import parsing.ast.Element
import parsing.ast.literals.Identifier
import source_structure.Position
import util.indent
import util.toLines

class Index(val target: Element, val indices: List<Element>, end: Position): Element(target.start, end) {

	override fun toString(): String {
		return "Index [ $target ] {${indices.toLines().indent()}\n}"
	}
}