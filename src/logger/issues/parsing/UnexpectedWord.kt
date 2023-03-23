package logger.issues.parsing

import logger.Issue
import logger.Severity
import source_structure.Section

class UnexpectedWord(override val text: String, section: Section): Issue(Severity.ERROR, section) {
	override val description = "The token doesn't make sense here."
}
