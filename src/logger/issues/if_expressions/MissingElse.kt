package logger.issues.if_expressions

import components.syntax_parser.syntax_tree.control_flow.IfExpression
import logger.Issue
import logger.Severity

class MissingElse(source: IfExpression): Issue(Severity.ERROR, source) {
	override val text = "The if expression is missing an else branch."
	override val description = "If expressions require an else branch."
	override val suggestion = "Add an else branch."
}
