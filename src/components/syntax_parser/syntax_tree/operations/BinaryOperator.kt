package components.syntax_parser.syntax_tree.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.operations.BinaryOperator as SemanticBinaryOperatorModel
import components.syntax_parser.syntax_tree.general.ValueElement
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
