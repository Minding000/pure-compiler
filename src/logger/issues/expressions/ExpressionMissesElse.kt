package logger.issues.expressions

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class ExpressionMissesElse(source: SyntaxTreeNode, type: String): Issue(Severity.ERROR, source) {
	override val text = "The $type expression is missing an else branch."
	override val description = "If and switch expressions require an else branch."
	override val suggestion = "Add an else branch."
}
