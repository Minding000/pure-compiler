package logger.issues.constant_conditions

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class FunctionCompletesWithoutReturning(source: SyntaxTreeNode, kind: String): Issue(Severity.ERROR, source) {
	override val text = "${kind.replaceFirstChar { it.titlecase() }} might complete without returning a value."
	override val description = "The $kind specifies a return type, but might complete without returning a value."
}
