package parsing.ast.definitions

import parsing.ast.general.MetaElement
import parsing.ast.literals.QuantifiedType
import parsing.ast.literals.Type
import source_structure.Position
import util.indent
import util.toLines

class LambdaParameterList(start: Position, val parameters: List<Type>, end: Position): MetaElement(start, end) {

	override fun toString(): String {
		return "LambdaParameterList {${parameters.toLines().indent()}\n}"
	}
}