package logger.issues.resolution

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class LiteralTypeNotFound(source: Element, name: String): Issue(Severity.ERROR, source) {
	override val isInternal = true
	override val text = "Literal type '$name' hasn't been declared yet."
	override val description = "The reference could not be resolved."
}
