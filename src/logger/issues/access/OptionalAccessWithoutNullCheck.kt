package logger.issues.access

import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class OptionalAccessWithoutNullCheck(source: SyntaxTreeNode, targetType: Type): Issue(Severity.ERROR, source) {
	override val text = "Cannot access member of optional type '$targetType' without null check."
	override val description = "The member is optional and therefore requires a null check to be accessed."
	override val suggestion = "Include null check in access."
}
