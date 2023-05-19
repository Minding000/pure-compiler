package logger.issues.returns

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class RedundantReturnValue(source: SyntaxTreeNode): Issue(Severity.WARNING, source) {
	override val text = "Return value is redundant."
	override val description = "The referenced callable doesn't defines a return type, so returns don't need a value."
}
