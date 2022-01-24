package parsing.ast.operations

import parsing.ast.Element
import util.indent
import util.toLines

class Assignment(val targets: List<Element>, val source: Element): Element(targets.first().start, source.end) {

	override fun toString(): String {
		return "Assignment {${"${targets.toLines()}\n= $source".indent()}\n}"
	}
}