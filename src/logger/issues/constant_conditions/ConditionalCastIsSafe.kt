package logger.issues.constant_conditions

import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class ConditionalCastIsSafe(source: SyntaxTreeNode, valueType: Type, referenceType: Type): Issue(Severity.WARNING, source) {
	override val text = "Cast from '$valueType' to '$referenceType' is safe."
	override val description = "The cast is guaranteed to succeed, but the outcome of the cast is checked."
	override val suggestion = "Change to safe cast."
}
