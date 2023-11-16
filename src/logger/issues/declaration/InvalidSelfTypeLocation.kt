package logger.issues.declaration

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class InvalidSelfTypeLocation(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "The self type is only allowed in type declarations."
	override val description = "The self type is only allowed in type declarations."
	override val suggestion = "Use a different type."
}
