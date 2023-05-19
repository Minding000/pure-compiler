package logger.issues.definition

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity
import logger.actions.RemoveElement

class InvalidInstanceLocation(source: SyntaxTreeNode): Issue(Severity.WARNING, source) {
	override val text = "Instance declarations are only allowed in enums and classes."
	override val description = "Instance declarations are only allowed in enums and classes."
	override val suggestion = "Remove instance."
	override val suggestedAction = RemoveElement(source)
}
