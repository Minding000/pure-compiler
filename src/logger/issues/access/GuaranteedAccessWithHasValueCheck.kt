package logger.issues.access

import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class GuaranteedAccessWithHasValueCheck(source: SyntaxTreeNode, targetType: Type): Issue(Severity.WARNING, source) {
	override val text = "Optional member access on guaranteed type '$targetType' is unnecessary."
	override val description = "The member is not optional and therefore doesn't require a has-value check to be accessed."
	override val suggestion = "Remove has-value check from access."
}
