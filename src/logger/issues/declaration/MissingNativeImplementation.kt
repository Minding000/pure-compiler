package logger.issues.declaration

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity
import util.uppercaseFirstChar

class MissingNativeImplementation(source: SyntaxTreeNode, type: String, signature: String): Issue(Severity.ERROR, source) {
	override val text = "${type.uppercaseFirstChar()} '${signature}' is missing a native implementation."
	override val description = "A native implementation is required."
	override val suggestion = "Add the missing native implementation."
}
