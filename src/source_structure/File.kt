package source_structure

import util.indent
import java.util.*

class File(val module: Module, val subPath: String, val name: String, val content: String) {
	val lines = ArrayList<Line>()

	init {
		var lineNumber = 0
		var startIndex = 0
		while(true) {
			lineNumber++
			val endIndex = content.indexOf("\n", startIndex)
			if(endIndex == -1) {
				lines.add(Line(this, startIndex, content.length, lineNumber))
				break
			}
			lines.add(Line(this, startIndex, endIndex, lineNumber))
			startIndex = endIndex + 1
		}
	}

	fun getFullName(): String {
		return "${module.name}::$subPath$name"
	}

	override fun toString(): String {
		return "File [$name] {${"\n$content".indent()}\n}"
	}
}