package logger.issues.definition

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class OperatorExpectedToNotReturn(source: Element): Issue(Severity.WARNING, source) {
	override val text = "This operator is not expected to return a value."
	override val description = "This operator is used in contexts where the return value is discarded."
	override val suggestion = "Remove the return type."
}
