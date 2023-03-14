package logger.issues.definition

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class DisallowedDeclarationType(source: Element, kind: String, context: String): Issue(Severity.ERROR, source) {
	override val text = "$kind declarations aren't allowed in '$context'."
	override val description = "This element is not allowed in the given context."
}
