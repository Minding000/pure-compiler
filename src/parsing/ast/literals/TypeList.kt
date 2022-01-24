package parsing.ast.literals

import parsing.ast.Element
import parsing.ast.definitions.TypeParameter
import source_structure.Position
import util.indent
import util.toLines

class TypeList(val typeParameters: List<TypeParameter>, start: Position, end: Position): Element(start, end) {

	override fun toString(): String {
		return "TypeList {${typeParameters.toLines().indent()}\n}"
	}
}