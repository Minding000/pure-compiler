package logger.issues.definition

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class ComputedVariableWithoutSetter(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Computed variable property needs to have a setter."
	override val description = "Computed variable properties need to have setters."
	override val suggestion = "Add a setter."
}
