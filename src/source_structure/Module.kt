package source_structure

import code.Main
import java.lang.StringBuilder
import java.util.*

class Module(val name: String) {
	val files = LinkedList<File>()

	fun addFile(name: String, content: String) {
		files.add(File(this, name, content))
	}

	override fun toString(): String {
		val string = StringBuilder()
		for(file in files)
			string.append("\n").append(file.toString())
		return "Module [$name] {${Main.indentText(string.toString())}\n}"
	}
}