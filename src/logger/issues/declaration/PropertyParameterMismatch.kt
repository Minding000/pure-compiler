package logger.issues.declaration

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class PropertyParameterMismatch(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Property parameter doesn't match any property."
	override val description = "Property parameters need to match a property by name."
}
