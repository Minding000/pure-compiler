package parsing.ast.operations

import parsing.ast.general.Element
import parsing.ast.general.MetaElement
import util.indent

class Cast(val value: Element, val operator: String, val type: Element): MetaElement(value.start, type.end) {

	override fun toString(): String {
		return "Cast {${"\n$value $operator $type".indent()}\n}"
	}
}