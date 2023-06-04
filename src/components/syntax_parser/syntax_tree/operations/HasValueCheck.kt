package components.syntax_parser.syntax_tree.operations

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import components.semantic_analysis.semantic_model.operations.HasValueCheck as SemanticHasValueCheckModel

class HasValueCheck(val identifier: Identifier): ValueSyntaxTreeNode(identifier.start, identifier.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticHasValueCheckModel {
		return SemanticHasValueCheckModel(this, scope, identifier.toSemanticModel(scope))
	}

	override fun toString(): String {
		return "HasValueCheck { $identifier }"
	}
}
