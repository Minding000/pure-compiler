package logger.issues.declaration

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class CircularInheritance(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Type definitions cannot inherit from themself."
	override val description = "The type definition directly or indirectly inherits from itself."
	override val suggestion = "Remove super type."
}
