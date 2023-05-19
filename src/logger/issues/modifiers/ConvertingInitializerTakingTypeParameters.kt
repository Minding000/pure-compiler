package logger.issues.modifiers

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class ConvertingInitializerTakingTypeParameters(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Converting initializers cannot take type parameters."
	override val description = "Converting initializers cannot take type parameters."
	override val suggestion = "Remove type parameters."
}
