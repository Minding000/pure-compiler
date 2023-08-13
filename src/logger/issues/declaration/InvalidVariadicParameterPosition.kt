package logger.issues.declaration

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class InvalidVariadicParameterPosition(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Variadic parameters have to be the last parameter."
	override val description = "Variadic parameters have to be placed at the end of the parameter list."
	override val suggestion = "Move variadic parameter to the end of the parameter list."
}
