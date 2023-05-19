package logger.issues.returns

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class ReturnStatementOutsideOfCallable(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Return statements are not allowed outside of callables."
	override val description = "Return statements require a callable to interact with."
}
