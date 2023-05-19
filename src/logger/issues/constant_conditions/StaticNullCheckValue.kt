package logger.issues.constant_conditions

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class StaticNullCheckValue(source: SyntaxTreeNode, value: String): Issue(Severity.WARNING, source) {
	override val text = "Null check always returns '$value'."
	override val description = "The null check is redundant, because it always returns the same value."
	override val suggestion = "Remove null check."
}
