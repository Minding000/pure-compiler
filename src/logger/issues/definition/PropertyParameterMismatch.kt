package logger.issues.definition

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class PropertyParameterMismatch(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Property parameter doesn't match any property."
	override val description = "Property parameters need to match a property by name."
}
