package logger.issues.access

import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class AbstractMonomorphicAccess(source: SyntaxTreeNode, memberType: String, signature: String, targetType: Type):
	Issue(Severity.ERROR, source) {
	override val text = "Monomorphic $memberType '$signature' accessed through abstract type '$targetType'."
	override val description = "Monomorphic members can only be accessed on specific (non-abstract) types."
	override val suggestion = "Add 'specific' modifier to target type."
}
