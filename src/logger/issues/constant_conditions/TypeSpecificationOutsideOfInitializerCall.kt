package logger.issues.constant_conditions

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class TypeSpecificationOutsideOfInitializerCall(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Type specifications can only be used on initializers."
	override val description = "Type specifications can only be used on generic types when calling their initializer."
}
