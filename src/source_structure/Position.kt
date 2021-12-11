package source_structure

class Position(val index: Int, val line: Line, val column: Int) {

	override fun toString(): String {
		return "${line.file.name}:${line.number}:$column"
	}
}