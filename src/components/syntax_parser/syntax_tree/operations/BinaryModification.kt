package components.syntax_parser.syntax_tree.operations

import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.MetaElement
import util.indent

class BinaryModification(val target: Element, val modifier: Element, val operator: String):
	MetaElement(target.start, modifier.end) {

	override fun toString(): String {
		return "BinaryModification {${"\n$target $operator $modifier".indent()}\n}"
	}
}
