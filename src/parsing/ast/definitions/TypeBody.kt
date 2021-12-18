package parsing.ast.definitions

import code.Main
import parsing.ast.Element
import parsing.ast.literals.Identifier
import source_structure.Position
import java.lang.StringBuilder

class TypeBody(start: Position, end: Position, val members: List<Element>): Element(start, end) {

	override fun toString(): String {
		val string = StringBuilder()
		for(member in members)
			string.append("\n").append(member.toString())
		return "TypeBody {${Main.indentText(string.toString())}\n}"
	}
}