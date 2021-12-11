package source_structure

class Line(val file: File, val start: Int, val end: Int, val number: Int) {

	fun getLength(): Int {
		return end - start
	}

	fun getContent(): String {
		return file.content.substring(start, end)
	}
}