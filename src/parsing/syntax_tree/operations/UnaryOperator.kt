package parsing.syntax_tree.operations

import linting.Linter
import linting.semantic_model.operations.UnaryOperator
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.ValueElement
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