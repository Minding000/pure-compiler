package parsing.ast.access

import code.Main
import parsing.ast.Element
import parsing.ast.literals.Identifier
import java.lang.StringBuilder

class ReferenceChain(val identifiers: List<Identifier>): Element(identifiers.first().start, identifiers.last().end) {

	override fun toString(): String {
		val string = StringBuilder()
		for(identifier in identifiers)
			string.append("\n").append(identifier.toString())
		return "ReferenceChain {${Main.indentText(string.toString())}\n}"
	}
}