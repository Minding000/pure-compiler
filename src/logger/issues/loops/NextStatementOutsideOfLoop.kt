package logger.issues.loops

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class NextStatementOutsideOfLoop(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Next statements are not allowed outside of loops."
	override val description = "Next statements require a loop to interact with."
}
