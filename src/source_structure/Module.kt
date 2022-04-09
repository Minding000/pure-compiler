package source_structure

import util.indent
import util.toLines
import java.util.*

class Module(val name: String) {
	val files = LinkedList<File>()

	fun addFile(pathParts: List<String>, name: String, content: String) {
		files.add(File(this, pathParts, name, content))
	}

	override fun toString(): String {
		return "Module [$name] {${files.toLines().indent()}\n}"
	}
}