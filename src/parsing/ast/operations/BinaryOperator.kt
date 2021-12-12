package parsing.ast.operations

import code.Main
import parsing.ast.Element

class BinaryOperator(val left: Element, val right: Element, val operator: String): Element(left.start, right.end) {

	override fun toString(): String {
		return "BinaryOperator {${Main.indentText("\n$left $operator $right")}\n}"
	}
}