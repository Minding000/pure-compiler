package parsing.ast.control_flow

import code.Main
import parsing.ast.Element
import source_structure.Position

class IfStatement(val condition: Element, val trueBranch: Element, val falseBranch: Element?, start: Position, end: Position): Element(start, end) {

	override fun toString(): String {
		return "If [$condition] {${Main.indentText("\n${trueBranch}")}\n}${if(falseBranch == null) "" else " Else {${Main.indentText("\n${falseBranch}")}\n}"}"
	}
}