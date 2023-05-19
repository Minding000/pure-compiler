package source_structure

import util.indent
import util.toLines
import java.util.*

class Module(val project: Project, val localName: String) {
	val files = LinkedList<File>()
	val publisher = "unknown"
	val remoteName = "unknown"
	val version = "unknown"

	fun addFile(pathParts: List<String>, name: String, content: String) {
		files.add(File(this, listOf(localName).plus(pathParts), name, content))
	}

	override fun toString(): String {
		return "Module [$localName] {${files.toLines().indent()}\n}"
	}
}
