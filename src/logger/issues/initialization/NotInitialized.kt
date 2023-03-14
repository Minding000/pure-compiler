package logger.issues.initialization

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class NotInitialized(source: Element, kind: String, signature: String): Issue(Severity.ERROR, source) {
	override val text = "$kind '$signature' hasn't been initialized yet."
	override val description = "The variable might not have been initialize when it is used."
	override val suggestion = "Initialize variable."
}
