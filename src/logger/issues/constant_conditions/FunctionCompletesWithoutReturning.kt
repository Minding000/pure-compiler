package logger.issues.constant_conditions

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity
import util.uppercaseFirstChar

class FunctionCompletesWithoutReturning(source: SyntaxTreeNode, kind: String): Issue(Severity.ERROR, source) {
	override val text = "${kind.uppercaseFirstChar()} might complete without returning a value."
	override val description = "The $kind specifies a return type, but might complete without returning a value."
}
