package logger.issues.switches

import components.semantic_model.control_flow.Case
import logger.Issue
import logger.Severity
import logger.actions.RemoveElement

class DuplicateCase(originalCase: Case, duplicateCase: Case): Issue(Severity.WARNING, duplicateCase.condition.source) {
	override val text = "Duplicated case '${duplicateCase.condition.source.getValue()}'," +
		" previously defined in ${originalCase.condition.source.getStartString()}."
	override val description = "There are multiple cases with the same condition. Only the first of these cases will be matched."
	override val suggestion = "Remove duplicate case."
	override val suggestedAction = RemoveElement(duplicateCase.source)
}
