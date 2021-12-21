package parsing.ast.general

import code.Main
import parsing.ast.Element
import source_structure.Position
import java.lang.StringBuilder

class AliasBlock(start: Position, end: Position, val aliases: List<Element>): Element(start, end) {

	override fun toString(): String {
		val aliasString = StringBuilder()
		for(alias in aliases)
			aliasString.append("\n").append(alias.toString())
		return "AliasBlock {${Main.indentText(aliasString.toString())}\n}"
	}
}