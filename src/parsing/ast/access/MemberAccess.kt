package parsing.ast.access

import parsing.ast.Element
import parsing.ast.literals.Identifier
import util.indent
import util.toLines

class MemberAccess(val left: Element, val right: Element, val isOptional: Boolean): Element(left.start, right.end) {

	override fun toString(): String {
		return "MemberAccess {${"\n$left${if(isOptional) "?." else "."}$right".indent()}\n}"
	}
}