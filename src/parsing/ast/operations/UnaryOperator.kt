package parsing.ast.operations

import linter.Linter
import linter.elements.operations.UnaryOperator
import linter.scopes.MutableScope
import parsing.ast.general.ValueElement
import parsing.tokenizer.Word

class UnaryOperator(val target: ValueElement, operator: Word): ValueElement(operator.start, target.end) {
	val operator = operator.getValue()

	override fun concretize(linter: Linter, scope: MutableScope): UnaryOperator {
		return UnaryOperator(this, target.concretize(linter, scope), operator)
	}

	override fun toString(): String {
		return "UnaryOperator { $operator$target }"
	}
}