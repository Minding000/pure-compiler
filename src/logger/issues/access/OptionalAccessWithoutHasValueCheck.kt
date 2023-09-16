package logger.issues.access

import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class OptionalAccessWithoutHasValueCheck(source: SyntaxTreeNode, targetType: Type): Issue(Severity.ERROR, source) {
	override val text = "Cannot access member of optional type '$targetType' without has-value check."
	override val description = "The member is optional and therefore requires a has-value check to be accessed."
	override val suggestion = "Include has-value check in access."
}
