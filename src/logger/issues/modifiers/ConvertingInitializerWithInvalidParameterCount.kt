package logger.issues.modifiers

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class ConvertingInitializerWithInvalidParameterCount(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Converting initializers have to take exactly one parameter."
	override val description = "Converting initializers have to take exactly one parameter."
}
