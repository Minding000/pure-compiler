package logger.issues.initialization

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class ConstantReassignment(source: SyntaxTreeNode, name: String): Issue(Severity.ERROR, source) {
	override val text = "'$name' cannot be reassigned, because it is constant."
	override val description = "Constants cannot be reassigned."
	override val suggestion = "Make '$name' variable."
}
