package components.syntax_parser.syntax_tree.operations

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.Operator
import components.syntax_parser.syntax_tree.general.ValueElement
import util.indent
import components.semantic_analysis.semantic_model.operations.BinaryOperator as SemanticBinaryOperatorModel

class BinaryOperator(private val left: ValueElement, private val right: ValueElement, private val operator: Operator):
	ValueElement(left.start, right.end) {

	override fun concretize(scope: MutableScope): SemanticBinaryOperatorModel {
		return SemanticBinaryOperatorModel(this, scope, left.concretize(scope), right.concretize(scope),
			operator.getKind())
	}

	override fun toString(): String {
		return "BinaryOperator {${"\n$left $operator $right".indent()}\n}"
	}
}
