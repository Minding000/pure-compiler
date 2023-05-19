package logger.issues.definition

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class ObjectInitializerTakingParameters(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Object initializers can not take parameters."
	override val description = "Object initializers can not take parameters, because they are called automatically."
	override val suggestion = "Remove parameters."
}
