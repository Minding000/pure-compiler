package parsing.ast.control_flow

import source_structure.Position
import parsing.ast.Element
import parsing.ast.literals.TypeList
import util.indent
import util.toLines

class FunctionCall(val functionReference: Element, val parameters: List<Element>, end: Position): Element(functionReference.start, end) {

	override fun toString(): String {
		return "FunctionCall [$functionReference] {${parameters.toLines().indent()}\n}"
	}
}