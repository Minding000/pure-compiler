package parsing.ast.literals

import parsing.ast.general.MetaElement
import source_structure.Position
import util.indent
import util.toLines

class TypeList(val typeParameters: List<Type>, start: Position, end: Position): MetaElement(start, end) {

	override fun toString(): String {
		return "TypeList {${typeParameters.toLines().indent()}\n}"
	}
}