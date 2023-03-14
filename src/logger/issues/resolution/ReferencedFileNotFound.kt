package logger.issues.resolution

import components.semantic_analysis.semantic_model.general.FileReference
import logger.Issue
import logger.Severity

class ReferencedFileNotFound(fileReference: FileReference): Issue(Severity.ERROR, fileReference.source) {
	override val text = "Failed to resolve file '${fileReference.identifier}'."
	override val description = "The referenced file has not been found."
}
