package components.syntax_parser.syntax_tree.access

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import source_structure.Position
import components.semantic_model.operations.InstanceAccess as SemanticInstanceAccessModel

class InstanceAccess(start: Position, val identifier: Identifier): ValueSyntaxTreeNode(start, identifier.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticInstanceAccessModel {
		return SemanticInstanceAccessModel(this, scope, identifier.getValue())
	}

	override fun toString(): String {
		return "InstanceAccess { $identifier }"
	}
}
