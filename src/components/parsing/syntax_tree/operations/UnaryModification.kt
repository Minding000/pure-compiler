package components.parsing.syntax_tree.operations

import linting.Linter
import linting.semantic_model.operations.UnaryModification as SemanticUnaryModificationModel
import linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.Element
import components.parsing.syntax_tree.general.ValueElement
import components.tokenizer.Word

class UnaryModification(val target: ValueElement, val operator: Word): Element(target.start, operator.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticUnaryModificationModel {
		return SemanticUnaryModificationModel(this, target.concretize(linter, scope), operator.getValue())
	}

	override fun toString(): String {
		return "UnaryModification { $target${operator.getValue()} }"
	}
}
