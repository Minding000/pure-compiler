package logger.issues.constant_conditions

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class StaticHasValueCheckResult(source: SyntaxTreeNode, value: String): Issue(Severity.WARNING, source) {
	override val text = "Has-value check always returns '$value'."
	override val description = "The has-value check is redundant, because it always returns the same value."
	override val suggestion = "Remove has-value check."
}
