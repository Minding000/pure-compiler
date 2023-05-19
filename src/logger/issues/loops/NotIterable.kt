package logger.issues.loops

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class NotIterable(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "The provided object is not iterable."
	override val description = "Only types implementing the 'Iterable' class can be iterated over."
}
