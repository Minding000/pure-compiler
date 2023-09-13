package logger.issues.loops

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity
import logger.actions.RemoveElement

class BreakStatementOutsideOfLoop(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Break statements are not allowed outside of loops."
	override val description = "Break statements require a loop to interact with."
	override val suggestedAction = RemoveElement(source)
}
