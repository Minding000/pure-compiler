package logger.issues.definition

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class ComputedPropertyMissingType(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Computed properties need to have an explicitly declared type."
	override val description = "Computed properties need to have an explicitly declared type."
	override val suggestion = "Specify a type."
}
