package components.syntax_parser.syntax_tree.operations

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.Operator
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import util.indent
import components.semantic_analysis.semantic_model.operations.BinaryOperator as SemanticBinaryOperatorModel

class BinaryOperator(private val left: ValueSyntaxTreeNode, private val right: ValueSyntaxTreeNode, private val operator: Operator):
	ValueSyntaxTreeNode(left.start, right.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticBinaryOperatorModel {
		return SemanticBinaryOperatorModel(this, scope, left.toSemanticModel(scope), right.toSemanticModel(scope),
			operator.getKind())
	}

	override fun toString(): String {
		return "BinaryOperator {${"\n$left $operator $right".indent()}\n}"
	}
}
