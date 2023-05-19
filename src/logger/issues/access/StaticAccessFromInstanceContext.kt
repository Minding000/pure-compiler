package logger.issues.access

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class StaticAccessFromInstanceContext(source: SyntaxTreeNode, name: String): Issue(Severity.WARNING, source) {
	override val text = "Accessing static member '$name' from instance context."
	override val description = "The member is available the static contexts, but is being accessed from an instance context."
	override val suggestion = "Access the member from the static context instead."
}
