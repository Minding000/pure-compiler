package logger.issues.resolution

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity
import logger.actions.RemoveElement

class InitializerReferenceOutsideOfInitializer(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Initializer references are not allowed outside of initializers."
	override val description = "Initializers can only be referenced from within initializers."
	override val suggestion = "Remove initializer reference."
	override val suggestedAction = RemoveElement(source)
}
