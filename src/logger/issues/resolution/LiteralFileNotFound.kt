package logger.issues.resolution

import logger.Issue
import logger.Severity

class LiteralFileNotFound(pathParts: List<String>): Issue(Severity.ERROR) {
	override val isInternal = true
	override val text = "Failed to get literal scope '${pathParts.joinToString(".")}'."
	override val description = "The source for a literal type has not been found."
}
