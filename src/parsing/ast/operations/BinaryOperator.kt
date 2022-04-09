package parsing.ast.operations

import parsing.ast.general.Element
import parsing.ast.general.MetaElement
import util.indent

class BinaryOperator(val left: Element, val right: Element, val operator: String): MetaElement(left.start, right.end) {

	override fun toString(): String {
		return "BinaryOperator {${"\n$left $operator $right".indent()}\n}"
	}
}