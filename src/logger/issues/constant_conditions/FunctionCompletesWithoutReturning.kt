package logger.issues.constant_conditions

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

//TODO: wording: also applies to operators
class FunctionCompletesWithoutReturning(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Function might complete without returning a value."
	override val description = "The function specifies a return type, but might complete without returning a value."
}
