package parsing.ast.control_flow

import parsing.ast.Element
import source_structure.Position
import util.indent
import util.toLines
import java.util.*

class SwitchStatement(val subject: Element, val cases: LinkedList<Case>, val elseBranch: Element?, start: Position, end: Position): Element(start, end) {

	override fun toString(): String {
		return "Switch [ $subject ] {${cases.toLines().indent()}\n}${if(elseBranch == null) "" else " Else {${"\n$elseBranch".indent()}\n}"}"
	}
}