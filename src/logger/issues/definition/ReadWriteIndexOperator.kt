package logger.issues.definition

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class ReadWriteIndexOperator(source: SyntaxTreeNode): Issue(Severity.WARNING, source) {
	override val text = "Index operators can not accept and return a value at the same time."
	override val description = "Index operators can only read or write. This should be reflected by their parameters and return types."
	override val suggestion = "Either read or write. To do both use overloading to define two separate implementations."
}
