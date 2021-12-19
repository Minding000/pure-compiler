package parsing.ast.control_flow

import code.Main
import parsing.ast.Element
import source_structure.Position
import java.lang.StringBuilder

class StatementBlock(start: Position, end: Position, val statements: List<Element>): Element(start, end) {

	override fun toString(): String {
		val string = StringBuilder()
		for(statement in statements)
			string.append("\n").append(statement.toString())
		return "StatementBlock {${Main.indentText(string.toString())}\n}"
	}
}