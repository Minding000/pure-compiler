package logger.issues.expressions

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class BranchMissesValue(source: SyntaxTreeNode, type: String): Issue(Severity.ERROR, source) {
	override val text = "This branch of the $type expression is missing a value."
	override val description = "All branches of if and switch expressions require a value as the last statement."
	override val suggestion = "Add an expression returning a value as the last statement of the branch."
}
