package parsing.ast.operations

import parsing.ast.general.Element
import parsing.ast.general.MetaElement
import parsing.ast.literals.Identifier

class NullCheck(val identifier: Identifier): MetaElement(identifier.start, identifier.end) {

	override fun toString(): String {
		return "NullCheck { $identifier }"
	}
}