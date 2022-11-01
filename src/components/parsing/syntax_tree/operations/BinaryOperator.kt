package components.parsing.syntax_tree.operations

import linting.Linter
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.operations.BinaryOperator as SemanticBinaryOperatorModel
import components.parsing.syntax_tree.general.ValueElement
import util.indent

class BinaryOperator(val left: ValueElement, val right: ValueElement, val operator: String):
	ValueElement(left.start, right.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticBinaryOperatorModel {
		return SemanticBinaryOperatorModel(this, left.concretize(linter, scope), right.concretize(linter, scope),
			operator)
	}

	override fun toString(): String {
		return "BinaryOperator {${"\n$left $operator $right".indent()}\n}"
	}
}
