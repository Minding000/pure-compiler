package logger.issues.constant_conditions

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class CastVariableAccessAfterIfStatement(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Cannot access cast variable after if statement."
	override val description = "The cast variable is only available in the branch where the cast succeeded."
}
