package logger.issues.definition

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class Declaration(source: Element, kind: String, signature: String): Issue(Severity.DEBUG, source) {
	override val isInternal = true
	override val text = "Declaration of $kind '$signature'."
	override val description = "The element has been declared."
}
