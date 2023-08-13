package logger.issues.declaration

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class ObjectInitializerTakingTypeParameters(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Object initializers can not take type parameters."
	override val description = "Object initializers can not take type parameters, because they are called automatically."
	override val suggestion = "Remove type parameters."
}
