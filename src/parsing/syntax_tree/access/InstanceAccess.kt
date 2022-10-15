package parsing.syntax_tree.access

import linting.Linter
import linting.semantic_model.operations.InstanceAccess
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.ValueElement
import parsing.syntax_tree.literals.Identifier
import source_structure.Position

class InstanceAccess(start: Position, val identifier: Identifier): ValueElement(start, identifier.end) {

	override fun concretize(linter: Linter, scope: MutableScope): InstanceAccess {
		return InstanceAccess(this, identifier.getValue())
	}

	override fun toString(): String {
		return "InstanceAccess { $identifier }"
	}
}
