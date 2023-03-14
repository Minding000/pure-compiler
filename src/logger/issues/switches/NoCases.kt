package logger.issues.switches

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity
import logger.actions.RemoveElement

class NoCases(source: Element): Issue(Severity.WARNING, source) {
	override val text = "The switch statement doesn't have any cases."
	override val description = "The switch statement has no effect, because it is empty."
	override val suggestion = "Remove switch statement."
	override val suggestedAction = RemoveElement(source)
}
