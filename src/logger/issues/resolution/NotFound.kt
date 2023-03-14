package logger.issues.resolution

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class NotFound(source: Element, kind: String, signature: String, override val isInternal: Boolean = false): Issue(Severity.ERROR, source) {
	override val text = "$kind '$signature' hasn't been declared yet."
	override val description = "The reference could not be resolved."
	override val suggestion = "Create the missing element."
}
