package logger.issues.definition

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class BinaryOperatorWithInvalidParameterCount(source: SyntaxTreeNode): Issue(Severity.WARNING, source) {
	override val text = "Binary operators need to accept exactly one parameter."
	override val description = "Binary operators act on the instance they belong to and one additional object."
}
