package parsing.ast.definitions

import parsing.ast.general.MetaElement
import source_structure.Position
import util.indent
import util.toLines

class ParameterList(start: Position, end: Position, val parameters: List<Parameter>): MetaElement(start, end) {

	override fun toString(): String {
		return "ParameterList {${parameters.toLines().indent()}\n}"
	}
}