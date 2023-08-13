package logger.issues.declaration

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class OperatorExpectedToReturn(source: SyntaxTreeNode): Issue(Severity.WARNING, source) {
	override val text = "This operator is expected to return a value."
	override val description = "This operator is used in contexts where a return value is required."
	override val suggestion = "Add a return type."
}
