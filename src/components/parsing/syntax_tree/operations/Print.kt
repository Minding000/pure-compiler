package components.parsing.syntax_tree.operations

import source_structure.Position
import components.parsing.syntax_tree.general.Element
import components.parsing.syntax_tree.general.MetaElement
import util.indent
import util.toLines

// NOTE: This instruction is only for development purposes
class Print(start: Position, end: Position, val elements: List<Element>): MetaElement(start, end) {

	override fun toString(): String {
		return "Print {${elements.toLines().indent()}\n}"
	}
}
