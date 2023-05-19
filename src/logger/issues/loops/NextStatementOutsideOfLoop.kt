package logger.issues.loops

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class NextStatementOutsideOfLoop(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Next statements are not allowed outside of loops."
	override val description = "Next statements require a loop to interact with."
}
