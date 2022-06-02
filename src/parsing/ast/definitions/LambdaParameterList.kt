package parsing.ast.definitions

import parsing.ast.general.MetaElement
import parsing.ast.general.TypeElement
import source_structure.Position
import util.indent
import util.toLines

class LambdaParameterList(start: Position, val parameters: List<TypeElement>, end: Position): MetaElement(start, end) {

	override fun toString(): String {
		return "LambdaParameterList {${parameters.toLines().indent()}\n}"
	}
}