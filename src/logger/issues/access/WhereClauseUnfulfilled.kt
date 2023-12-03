package logger.issues.access

import components.semantic_model.declarations.WhereClause
import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class WhereClauseUnfulfilled(source: SyntaxTreeNode, memberType: String, signature: String, targetType: Type, whereClause: WhereClause):
	Issue(Severity.ERROR, source) {
	override val text = "$memberType '$signature' cannot be accessed on object of type '$targetType'," +
		" because the condition '$whereClause' is not met."
	override val description = "Members with where clause are only accessible if the type condition specified in the where clause is met."
}
