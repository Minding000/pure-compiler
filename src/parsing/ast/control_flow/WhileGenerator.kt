package parsing.ast.control_flow

import parsing.ast.Element
import parsing.ast.general.StatementBlock
import source_structure.Position
import util.indent

class WhileGenerator(start: Position, val condition: Element, val isPostCondition: Boolean): Element(start, condition.end) {

	override fun toString(): String {
		return "WhileGenerator [${if(isPostCondition) "post" else "pre"}] {${"\n$condition".indent()}\n}"
	}
}