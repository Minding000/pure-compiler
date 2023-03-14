package logger.issues.definition

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class DeclarationMissingTypeOrValue(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Declaration requires a type or value to infer a type from."
	override val description = "Declarations require either a type specified directly or value to infer a type from."
	override val suggestion = "Add a type or value."
}
