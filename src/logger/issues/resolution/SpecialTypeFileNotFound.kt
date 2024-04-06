package logger.issues.resolution

import logger.Issue
import logger.Severity

class SpecialTypeFileNotFound(pathParts: List<String>): Issue(Severity.ERROR) {
	override val isInternal = true
	override val text = "Failed to get special type scope '${pathParts.joinToString(".")}'."
	override val description = "The source for a special type has not been found."
}
