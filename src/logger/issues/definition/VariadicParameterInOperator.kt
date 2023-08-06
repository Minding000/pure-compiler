package logger.issues.definition

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class VariadicParameterInOperator(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Variadic parameter in operator definition."
	override val description = "Operators do not support variadic parameters."
	override val suggestion = "Remove variadic parameters."
}
