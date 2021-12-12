package parsing.ast.operations

import parsing.ast.Element
import parsing.tokenizer.Word

class UnaryModification(val target: Element, operator: Word): Element(target.start, operator.end) {
	val operator = operator.getValue()

	override fun toString(): String {
		return "UnaryModification { $target$operator }"
	}
}