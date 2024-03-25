package logger.issues.constant_conditions

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity
import util.uppercaseFirstChar

class FunctionCompletesDespiteNever(source: SyntaxTreeNode, kind: String): Issue(Severity.ERROR, source) {
	override val text = "${kind.uppercaseFirstChar()} might complete despite of 'Never' return type."
	override val description = "The $kind specifies the 'Never' return type, but might complete."
}
