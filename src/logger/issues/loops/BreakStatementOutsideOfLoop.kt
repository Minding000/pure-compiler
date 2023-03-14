package logger.issues.loops

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class BreakStatementOutsideOfLoop(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Break statements are not allowed outside of loops."
	override val description = "Break statements require a loop to interact with."
}
