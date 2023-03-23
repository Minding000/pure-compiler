package logger.issues.parsing

import logger.Issue
import logger.Severity
import source_structure.Section

class UnexpectedEndOfFile(override val text: String, section: Section): Issue(Severity.ERROR, section) {
	override val description = "The file ended without completing all statements."
}
