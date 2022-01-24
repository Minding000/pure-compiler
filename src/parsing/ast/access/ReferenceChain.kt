package parsing.ast.access

import parsing.ast.Element
import parsing.ast.literals.Identifier
import util.indent
import util.toLines

class ReferenceChain(val identifiers: List<Identifier>): Element(identifiers.first().start, identifiers.last().end) {

	override fun toString(): String {
		return "ReferenceChain {${identifiers.toLines().indent()}\n}"
	}
}