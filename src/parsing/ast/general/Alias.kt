package parsing.ast.general

import parsing.ast.Element
import parsing.ast.literals.Identifier

class Alias(val originalName: Identifier, val aliasName: Identifier): Element(originalName.start, aliasName.end) {

	override fun toString(): String {
		return "Alias { $originalName as $aliasName }"
	}
}