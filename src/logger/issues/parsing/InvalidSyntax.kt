package logger.issues.parsing

import logger.Issue
import logger.Severity
import source_structure.Section

class InvalidSyntax(override val text: String, section: Section? = null): Issue(Severity.ERROR, section) {
	override val description = "The syntax is invalid."
}
