package source_structure

import java.util.*

class File(val project: Project, val name: String, val content: String) {
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
}