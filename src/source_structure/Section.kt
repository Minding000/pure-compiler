package source_structure

import java.lang.StringBuilder

open class Section(val start: Position, val end: Position): IdentifierSource {
	private val file: File
		get() = start.line.file
	val length: Int
		get() = end.index - start.index

	final override fun getValue(): String {
		return file.content.substring(start.index, end.index)
	}

	override fun getStartString(): String {
		return start.toString()
	}

	override fun getRegionString(): String {
		return "$start-${end.line.number}:${end.column}"
	}

	fun getHighlight(): String {
		val highlight = StringBuilder()
		val lines = file.lines
		for(i in start.line.number - 1 until end.line.number) {
			val line = lines[i]
			highlight.append(line.getContent().replace("\t", " "))
			highlight.append("\n")
			val highlightStart = if(line == start.line) start.column else 0
			val highlightEnd = if(line == end.line) end.column else line.end
			highlight.append(" ".repeat(highlightStart))
			highlight.append("^".repeat(highlightEnd - highlightStart))
		}
		return highlight.toString()
	}
}
