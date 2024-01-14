package logger.issues.if_expressions

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class MissingValue(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "This branch of the if expression is missing a value."
	override val description = "All branches of if expressions require a value as the last statement."
	override val suggestion = "Add an expression returning a value as the last statement of the branch."
}
