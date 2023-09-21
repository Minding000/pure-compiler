package components.syntax_parser.syntax_tree.operations

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.semantic_model.operations.HasValueCheck as SemanticHasValueCheckModel

class HasValueCheck(val subject: ValueSyntaxTreeNode): ValueSyntaxTreeNode(subject.start, subject.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticHasValueCheckModel {
		return SemanticHasValueCheckModel(this, scope, subject.toSemanticModel(scope))
	}

	override fun toString(): String {
		return "HasValueCheck { $subject }"
	}
}
