package logger.issues.definition

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class ParameterInUnaryOperator(source: SyntaxTreeNode): Issue(Severity.WARNING, source) {
	override val text = "Unary operators can't accept parameters."
	override val description = "Unary operators only act on the instance they belong to."
	override val suggestion = "Remove all parameters."
}
