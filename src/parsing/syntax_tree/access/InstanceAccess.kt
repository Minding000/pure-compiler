package parsing.syntax_tree.access

import linting.Linter
import linting.semantic_model.access.InstanceAccess
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.ValueElement
import parsing.syntax_tree.literals.Identifier

class InstanceAccess(val identifier: Identifier): ValueElement(identifier.start, identifier.end) {

	override fun concretize(linter: Linter, scope: MutableScope): InstanceAccess {
		return InstanceAccess(this, identifier.getValue())
	}

	override fun toString(): String {
		return "InstanceAccess { $identifier }"
	}
}
