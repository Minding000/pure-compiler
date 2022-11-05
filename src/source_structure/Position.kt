package source_structure

class Position(val index: Int, val line: Line, val column: Int) {

	override fun toString(): String {
		return "${line.file.getIdentifier()}:${line.number}:$column"
	}

	operator fun compareTo(other: Position): Int {
		return index - other.index
	}
}
