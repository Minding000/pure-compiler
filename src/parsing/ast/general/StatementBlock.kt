package parsing.ast.general

import code.Main
import parsing.ast.Element
import source_structure.Position
import java.lang.StringBuilder

class StatementBlock(start: Position, end: Position, val statements: List<Element>): Element(start, end) {

	override fun toString(): String {
		val statementString = StringBuilder()
		for(statement in statements)
			statementString.append("\n").append(statement.toString())
		return "StatementBlock {${Main.indentText(statementString.toString())}\n}"
	}
}