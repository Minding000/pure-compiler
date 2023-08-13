package logger.issues.declaration

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class MultipleVariadicParameters(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Signatures can have at most one variadic parameter."
	override val description = "Multiple variadic parameters are provided but at most one variadic parameter is allowed."
	override val suggestion = "Remove extraneous variadic parameters."
}
