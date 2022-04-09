package parsing.ast.operations

import parsing.ast.general.Element
import parsing.ast.general.MetaElement
import util.indent

class BinaryModification(val target: Element, val modifier: Element, val operator: String): MetaElement(target.start, modifier.end) {

	override fun toString(): String {
		return "BinaryModification {${"\n$target $operator $modifier".indent()}\n}"
	}
}