package parsing.ast.operations

import parsing.ast.Element
import parsing.ast.literals.Identifier

class NullCheck(val identifier: Identifier): Element(identifier.start, identifier.end) {

	override fun toString(): String {
		return "NullCheck { $identifier }"
	}
}