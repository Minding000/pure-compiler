package parsing.ast.control_flow

import parsing.ast.Element
import source_structure.Position
import util.indent

class IfStatement(val condition: Element, val trueBranch: Element, val falseBranch: Element?, start: Position, end: Position): Element(start, end) {

	override fun toString(): String {
		return "If [ $condition ] {${"\n${trueBranch}".indent()}\n}${if(falseBranch == null) "" else " Else {${"\n${falseBranch}".indent()}\n}"}"
	}
}