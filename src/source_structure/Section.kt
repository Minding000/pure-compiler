package source_structure

import java.lang.StringBuilder

open class Section(val start: Position, val end: Position): IdentifierSource {

	fun getFile(): File {
		return start.line.file
	}

	final override fun getValue(): String {
		return getFile().content.substring(start.index, end.index)
	}

	override fun getStartString(): String {
		return start.toString()
	}

	override fun getRegionString(): String {
		return "$start-${end.line.number}:${end.column}"
	}

	fun getHighlight(): String {
		val sb = StringBuilder()
		val lines = getFile().lines
		for(i in start.line.number - 1 until end.line.number) {
			val line = lines[i]
			sb.append(line.getContent().replace("\t", " "))
			sb.append("\n")
			val highlightStart = if(line == start.line) start.column else 0
			val highlightEnd = if(line == end.line) end.column else line.end
			sb.append(" ".repeat(highlightStart))
			sb.append("^".repeat(highlightEnd - highlightStart))
		}
		return sb.toString()
	}
}