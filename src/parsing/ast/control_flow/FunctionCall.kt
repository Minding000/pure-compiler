package parsing.ast.control_flow

import source_structure.Position
import parsing.ast.Element
import parsing.ast.literals.TypeList
import util.indent
import util.toLines

class FunctionCall(val typeList: TypeList?, val functionReference: Element, val parameters: List<Element>, start: Position, end: Position): Element(start, end) {

	override fun toString(): String {
		return "FunctionCall [${if(typeList != null) "$typeList " else ""}$functionReference] {${parameters.toLines().indent()}\n}"
	}
}