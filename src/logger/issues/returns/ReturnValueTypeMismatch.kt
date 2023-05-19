package logger.issues.returns

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class ReturnValueTypeMismatch(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Return value doesn't match the declared return type."
	override val description = "The referenced callable defines a return type, but the returned value is of a different type."
}
