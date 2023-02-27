package components.syntax_parser.syntax_tree.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.Operator
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.ValueElement
import components.semantic_analysis.semantic_model.operations.UnaryModification as SemanticUnaryModificationModel

class UnaryModification(val target: ValueElement, val operator: Operator): Element(target.start, operator.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticUnaryModificationModel {
		return SemanticUnaryModificationModel(this, scope, target.concretize(linter, scope), operator.getKind())
	}

	override fun toString(): String {
		return "UnaryModification { $target $operator }"
	}
}
