package logger.issues.constant_conditions

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class FunctionCompletesDespiteNever(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Function might complete despite of 'Never' return type."
	override val description = "The function specifies the 'Never' return type, but might complete."
}
