package parsing.ast.operations

import linter.Linter
import linter.elements.operations.NullCheck
import linter.scopes.Scope
import parsing.ast.general.ValueElement
import parsing.ast.literals.Identifier

class NullCheck(val identifier: Identifier): ValueElement(identifier.start, identifier.end) {

	override fun concretize(linter: Linter, scope: Scope): NullCheck {
		return NullCheck(this, identifier.concretize(linter, scope))
	}

	override fun toString(): String {
		return "NullCheck { $identifier }"
	}
}