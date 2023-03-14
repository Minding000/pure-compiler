package logger.issues.constant_conditions

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class CastVariableWithoutIs(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Cannot declare a variable here."
	override val description = "Only 'is' casts can declare a variable."
	override val suggestion = "Remove variable declaration."
}
