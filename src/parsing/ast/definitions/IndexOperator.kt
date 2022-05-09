package parsing.ast.definitions

import source_structure.Position
import util.indent
import util.toLines

class IndexOperator(start: Position, end: Position, val parameters: List<Parameter>): Operator(start, end) {

	fun getSignature(): String {
		return "[${parameters.joinToString { parameter -> parameter.getTypeName() }}]"
	}

	override fun toString(): String {
		return "IndexOperator {${parameters.toLines().indent()}\n}"
	}
}