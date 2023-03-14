package logger.issues.constant_conditions

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class UnreachableStatement(source: Element): Issue(Severity.WARNING, source) {
	override val text = "Statement is unreachable."
	override val description = "The control flow will never reach this statement."
}
