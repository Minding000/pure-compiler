package parsing.ast.operations

import parsing.ast.Element
import util.indent

class BinaryOperator(val left: Element, val right: Element, val operator: String): Element(left.start, right.end) {

	override fun toString(): String {
		return "BinaryOperator {${"\n$left $operator $right".indent()}\n}"
	}
}