package parsing.syntax_tree.definitions

import source_structure.Position
import util.indent
import util.toLines

class IndexOperator(start: Position, end: Position, val indices: List<Parameter>): Operator(start, end) {

	override fun toString(): String {
		return "IndexOperator {${indices.toLines().indent()}\n}"
	}
}