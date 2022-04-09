package parsing.ast.access

import linter.Linter
import linter.elements.access.InstanceAccess
import linter.scopes.Scope
import parsing.ast.general.Element
import parsing.ast.literals.Identifier

class InstanceAccess(val identifier: Identifier): Element(identifier.start, identifier.end) {

	override fun concretize(linter: Linter, scope: Scope): InstanceAccess {
		return InstanceAccess(this, identifier.concretize(linter, scope))
	}

	override fun toString(): String {
		return "InstanceAccess { $identifier }"
	}
}