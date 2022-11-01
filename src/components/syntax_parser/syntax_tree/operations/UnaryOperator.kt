package components.syntax_parser.syntax_tree.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.operations.UnaryOperator as SemanticUnaryOperatorModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueElement
import components.tokenizer.Word

class UnaryOperator(val target: ValueElement, operator: Word): ValueElement(operator.start, target.end) {
	val operator = operator.getValue()

	override fun concretize(linter: Linter, scope: MutableScope): SemanticUnaryOperatorModel {
		return SemanticUnaryOperatorModel(this, target.concretize(linter, scope), operator)
	}

	override fun toString(): String {
		return "UnaryOperator { $operator$target }"
	}
}
