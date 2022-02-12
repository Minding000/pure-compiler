package parsing.ast.access

import parsing.ast.Element
import parsing.ast.literals.Identifier
import parsing.tokenizer.Word

class InstanceAccess(val identifier: Identifier): Element(identifier.start, identifier.end) {

	override fun toString(): String {
		return "InstanceAccess { $identifier }"
	}
}