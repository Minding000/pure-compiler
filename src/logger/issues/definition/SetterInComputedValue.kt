package logger.issues.definition

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity
import logger.actions.RemoveElement

class SetterInComputedValue(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Computed value property cannot have a setter."
	override val description = "Computed value properties cannot have setters, because they cannot be reassigned."
	override val suggestion = "Remove setter."
	override val suggestedAction = RemoveElement(source)
}
