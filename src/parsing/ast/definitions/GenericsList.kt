package parsing.ast.definitions

import code.Main
import parsing.ast.Element
import parsing.ast.literals.Identifier
import source_structure.Position
import java.lang.StringBuilder

class GenericsList(val identifiers: List<Identifier>, start: Position, end: Position): Element(start, end) {

	override fun toString(): String {
		val string = StringBuilder()
		for(identifier in identifiers)
			string.append("\n").append(identifier.toString())
		return "GenericsList {${Main.indentText(string.toString())}\n}"
	}
}