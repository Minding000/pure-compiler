package logger.issues.constant_conditions

import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class UnsafeSafeCast(source: SyntaxTreeNode, valueType: Type, referenceType: Type): Issue(Severity.ERROR, source) {
	override val text = "Cannot safely cast '$valueType' to '$referenceType'."
	override val description = "The cast is not guaranteed to succeed and the outcome of the cast is not checked."
	override val suggestion = "Change to conditional cast."
}
