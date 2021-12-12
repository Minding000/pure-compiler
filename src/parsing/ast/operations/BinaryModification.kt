package parsing.ast.operations

import code.Main
import parsing.ast.Element

class BinaryModification(val target: Element, val modifier: Element, val operator: String): Element(target.start, modifier.end) {

	override fun toString(): String {
		return "BinaryModification {${Main.indentText("\n$target $operator $modifier")}\n}"
	}
}