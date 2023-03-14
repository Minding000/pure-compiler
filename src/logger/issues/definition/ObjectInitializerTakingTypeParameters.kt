package logger.issues.definition

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class ObjectInitializerTakingTypeParameters(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Object initializers can not take type parameters."
	override val description = "Object initializers can not take type parameters, because they are called automatically."
	override val suggestion = "Remove type parameters."
}
