package logger.issues.resolution

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class SuperMemberNotFound(source: Element): Issue(Severity.ERROR, source) {
	override val text = "The specified member does not exist on any super type of this type definition."
	override val description = "The specified member does not exist on any explicitly or implicitly inherited type."
	override val suggestion = "Create the missing element."
}
