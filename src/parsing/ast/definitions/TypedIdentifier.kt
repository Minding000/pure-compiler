package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.TypedIdentifier
import linter.scopes.Scope
import parsing.ast.general.Element
import parsing.ast.literals.Identifier
import parsing.ast.literals.Type

class TypedIdentifier(val identifier: Identifier, val type: Type): Element(identifier.start, type.end) {

	override fun concretize(linter: Linter, scope: Scope): TypedIdentifier {
		val typedIdentifier = TypedIdentifier(this, identifier.getValue(), type.concretize(linter, scope))
		scope.declareValue(typedIdentifier)
		return typedIdentifier
	}

	override fun toString(): String {
		return "TypedIdentifier { $identifier: $type }"
	}
}