package parsing.ast.operations

import parsing.ast.general.Element
import parsing.ast.general.MetaElement
import util.indent
import util.toLines

class Assignment(val targets: List<Element>, val source: Element): MetaElement(targets.first().start, source.end) {

	override fun toString(): String {
		return "Assignment {${"${targets.toLines()}\n= $source".indent()}\n}"
	}
}