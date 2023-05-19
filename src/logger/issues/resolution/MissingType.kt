package logger.issues.resolution

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class MissingType(source: SyntaxTreeNode): Issue(Severity.WARNING, source) {
	override val isInternal = true
	override val text = "Failed to resolve type of value '${source.getValue()}'."
	override val description = "This should be reported by a more specific error already."
}
