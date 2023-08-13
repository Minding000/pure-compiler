package logger.issues.declaration

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity
import logger.actions.RemoveElement

class SetterInComputedValue(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Computed value property cannot have a setter."
	override val description = "Computed value properties cannot have setters, because they cannot be reassigned."
	override val suggestion = "Remove setter."
	override val suggestedAction = RemoveElement(source)
}
