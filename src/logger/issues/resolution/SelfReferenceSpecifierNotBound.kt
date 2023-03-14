package logger.issues.resolution

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class SelfReferenceSpecifierNotBound(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Self references can only specify types they are bound to."
	override val description = "Self references need to be bound to the type they specify."
}
