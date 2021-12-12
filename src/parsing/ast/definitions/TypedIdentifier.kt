package parsing.ast.definitions

import parsing.ast.Element
import parsing.ast.literals.Identifier

class TypedIdentifier(val identifier: Identifier, val typeIdentifier: Identifier): Element(identifier.start, typeIdentifier.end) {

	override fun toString(): String {
		return "TypedIdentifier { $identifier : $typeIdentifier }"
	}
}