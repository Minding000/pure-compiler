package logger.issues.resolution

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class SuperInitializerCallOutsideOfInitializer(source: Element): Issue(Severity.ERROR, source) {
	override val text = "The super initializer can only be called from initializers."
	override val description = "Super initializers can only be called from within initializers."
}
