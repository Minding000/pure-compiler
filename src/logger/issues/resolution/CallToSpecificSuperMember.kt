package logger.issues.resolution

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class CallToSpecificSuperMember(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Super calls to specific functions are not allowed."
	override val description = "Super calls to specific functions are not allowed."
}
