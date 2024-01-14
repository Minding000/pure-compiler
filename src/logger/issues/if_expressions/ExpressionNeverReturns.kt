package logger.issues.if_expressions

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class ExpressionNeverReturns(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "This expression never returns a value."
	override val description = "This expression is supposed to provide a value, but it never returns one."
	override val suggestion = "Ensure the expression returns a value."
}
