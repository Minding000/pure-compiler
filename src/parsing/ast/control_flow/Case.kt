package parsing.ast.control_flow

import parsing.ast.Element
import util.indent

class Case(val condition: Element, val result: Element): Element(condition.start, result.end) {

	override fun toString(): String {
		return "Case [ $condition ] {${"\n$result".indent()}\n}"
	}
}