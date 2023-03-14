package logger.issues.returns

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class ReturnValueTypeMismatch(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Return value doesn't match the declared return type."
	override val description = "The referenced callable defines a return type, but the returned value is of a different type."
}
