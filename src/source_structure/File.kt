package source_structure

import util.indent
import java.util.*

class File(val module: Module, val pathParts: List<String>, val name: String, val content: String) {
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

	fun getIdentifier(): String {
		val identifier = java.lang.StringBuilder()
		for(pathPart in pathParts)
			identifier.append(pathPart).append('.')
		identifier.append(name)
		return identifier.toString()
	}

	fun getStart(): Position {
		return Position(0, lines.first(), 0)
	}

	fun getEnd(): Position {
		val line = lines.last()
		return Position(content.length, line, line.getLength())
	}

	override fun toString(): String {
		return "File [$name] {${"\n$content".indent()}\n}"
	}
}