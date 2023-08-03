package logger.issues.resolution

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class SuperMemberNotFound(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "The specified member does not exist on any super type of this type definition."
	override val description = "The specified member does not exist on any explicitly or implicitly inherited type."
	override val suggestion = "Create the missing member."
}
