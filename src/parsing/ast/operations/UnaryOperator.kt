package parsing.ast.operations

import parsing.ast.general.Element
import parsing.ast.general.MetaElement
import parsing.tokenizer.Word

class UnaryOperator(val target: Element, operator: Word): MetaElement(operator.start, target.end) {
	val operator = operator.getValue()

	override fun toString(): String {
		return "UnaryOperator { $operator$target }"
	}
}