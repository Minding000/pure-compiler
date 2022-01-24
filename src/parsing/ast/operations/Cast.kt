package parsing.ast.operations

import parsing.ast.Element
import util.indent

class Cast(val value: Element, val operator: String, val type: Element): Element(value.start, type.end) {

	override fun toString(): String {
		return "Cast {${"\n$value $operator $type".indent()}\n}"
	}
}