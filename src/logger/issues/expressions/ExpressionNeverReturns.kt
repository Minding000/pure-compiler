package logger.issues.expressions

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class ExpressionNeverReturns(source: SyntaxTreeNode, type: String): Issue(Severity.ERROR, source) {
	override val text = "This $type expression never returns a value."
	override val description = "This expression is supposed to provide a value, but it never returns one."
	override val suggestion = "Ensure the expression returns a value."
}
