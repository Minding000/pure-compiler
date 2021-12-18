package parsing.ast.literals

import code.Main
import parsing.ast.Element
import source_structure.Position
import java.lang.StringBuilder

class TypeList(val types: List<Type>, start: Position, end: Position): Element(start, end) {

	override fun toString(): String {
		val string = StringBuilder()
		for(type in types)
			string.append("\n").append(type.toString())
		return "TypeList {${Main.indentText(string.toString())}\n}"
	}
}