package parsing.ast.definitions

import source_structure.Position
import util.indent
import util.toLines

class IndexOperator(start: Position, end: Position, private val parameters: List<TypedIdentifier>): Operator(start, end) {

	override fun toString(): String {
		return "IndexOperator {${parameters.toLines().indent()}\n}"
	}
}