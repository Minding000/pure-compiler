package components.parsing.syntax_tree.access

import linting.Linter
import linting.semantic_model.operations.InstanceAccess as SemanticInstanceAccessModel
import linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.ValueElement
import components.parsing.syntax_tree.literals.Identifier
import source_structure.Position

class InstanceAccess(start: Position, val identifier: Identifier): ValueElement(start, identifier.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticInstanceAccessModel {
		return SemanticInstanceAccessModel(this, identifier.getValue())
	}

	override fun toString(): String {
		return "InstanceAccess { $identifier }"
	}
}
