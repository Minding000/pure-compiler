package logger.issues.resolution

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class NotFound(source: SyntaxTreeNode, kind: String, signature: String): Issue(Severity.ERROR, source) {
	override val text = "$kind '$signature' hasn't been declared yet."
	override val description = "The reference could not be resolved."
	override val suggestion = "Create the missing element."
}
