package logger.issues.tokenization

import logger.Issue
import logger.Severity
import source_structure.Position
import source_structure.Section
import util.stringify

class UnknownWord(val position: Position):
	Issue(Severity.ERROR, Section(position, Position(position.index, position.line, position.column + 1))) {
	override val text: String
		get() {
			val file = position.line.file
			return "Unknown word in ${file.name}:${position.line.number}:${position.column}: '${file.content[position.index].stringify()}'."
		}
	override val description = "The character has not been recognized as being part of a token."
}
