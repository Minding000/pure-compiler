package logger.issues.returns

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class ReturnStatementOutsideOfCallable(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Return statements are not allowed outside of callables."
	override val description = "Return statements require a callable to interact with."
}
