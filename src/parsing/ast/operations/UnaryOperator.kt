package parsing.ast.operations

import parsing.ast.Element
import parsing.tokenizer.Word

class UnaryOperator(val target: Element, operator: Word): Element(operator.start, target.end) {
	val operator = operator.getValue()

	override fun toString(): String {
		return "UnaryOperator { $operator$target }"
	}
}