package parsing.ast.general

import parsing.ast.Element
import source_structure.Position
import util.indent
import util.toLines

class AliasBlock(start: Position, end: Position, val aliases: List<Element>): Element(start, end) {

	override fun toString(): String {
		return "AliasBlock {${aliases.toLines().indent()}\n}"
	}
}