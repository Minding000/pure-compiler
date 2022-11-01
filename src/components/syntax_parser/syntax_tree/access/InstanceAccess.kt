package components.syntax_parser.syntax_tree.access

import components.linting.Linter
import components.linting.semantic_model.operations.InstanceAccess as SemanticInstanceAccessModel
import components.linting.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueElement
import components.syntax_parser.syntax_tree.literals.Identifier
import source_structure.Position

class InstanceAccess(start: Position, val identifier: Identifier): ValueElement(start, identifier.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticInstanceAccessModel {
		return SemanticInstanceAccessModel(this, identifier.getValue())
	}

	override fun toString(): String {
		return "InstanceAccess { $identifier }"
	}
}
