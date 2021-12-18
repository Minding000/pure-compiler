package parsing.ast.definitions

import parsing.ast.Element
import parsing.ast.literals.Identifier
import parsing.ast.literals.Type

class TypedIdentifier(val identifier: Identifier, val type: Type): Element(identifier.start, type.end) {

	override fun toString(): String {
		return "TypedIdentifier { $identifier : $type }"
	}
}