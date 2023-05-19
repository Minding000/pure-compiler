package logger.issues.returns

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class ReturnStatementMissingValue(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Return statement needs a value."
	override val description = "The referenced callable defines a return type, so all returns need to have a value of that type."
}
