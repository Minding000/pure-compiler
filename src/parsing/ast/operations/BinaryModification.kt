package parsing.ast.operations

import parsing.ast.Element
import util.indent

class BinaryModification(val target: Element, val modifier: Element, val operator: String): Element(target.start, modifier.end) {

	override fun toString(): String {
		return "BinaryModification {${"\n$target $operator $modifier".indent()}\n}"
	}
}