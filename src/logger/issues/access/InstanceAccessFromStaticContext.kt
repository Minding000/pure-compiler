package logger.issues.access

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class InstanceAccessFromStaticContext(source: Element, name: String): Issue(Severity.ERROR, source) {
	override val text = "Cannot access instance member '$name' from static context."
	override val description = "The member is only available in instance contexts, but is being accessed from the static context."
	override val suggestion = "Access the member from an instance context instead."
}
