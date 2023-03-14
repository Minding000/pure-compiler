package logger.issues.definition

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class ShadowsElement(source: Element, kind: String, name: String, previousSource: Element): Issue(Severity.WARNING, source) {
	override val text = "'$name' shadows a $kind, previously declared in ${previousSource.getStartString()}."
	override val description = "The element shadows another element with the same name."
}
