package parsing.ast.operations

import code.Main
import parsing.ast.Element

class Assignment(val target: Element, val source: Element): Element(target.start, source.end) {

	override fun toString(): String {
		return "Assignment {${Main.indentText("\n$target = $source")}\n}"
	}
}