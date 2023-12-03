package logger.issues.declaration

import components.semantic_model.declarations.WhereClauseCondition
import logger.Issue
import logger.Severity

class InvalidWhereClauseSubject(whereClauseCondition: WhereClauseCondition, typeDeclarationName: String):
	Issue(Severity.WARNING, whereClauseCondition.subject.source) {
	override val text = "Condition '$whereClauseCondition' has no effect," +
		" because subject '${whereClauseCondition.subject}' is not a generic type of '$typeDeclarationName'."
	override val description = "Where clause condition subjects need to reference the generic types of the parent type declaration."
	override val suggestion = "Use a generic type of the parent type declaration as the subject."
}
