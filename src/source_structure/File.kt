package source_structure

import code.Main
import java.util.*

class File(val module: Module, val name: String, val content: String) {
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

	override fun toString(): String {
		return "File [$name] {${Main.indentText("\n$content")}\n}"
	}
}