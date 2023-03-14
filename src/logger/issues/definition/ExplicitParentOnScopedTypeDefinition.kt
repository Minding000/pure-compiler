package logger.issues.definition

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity
import logger.actions.RemoveElement

class ExplicitParentOnScopedTypeDefinition(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Explicit parent types are only allowed on unscoped type definitions."
	override val description = "The parent type can only be explicitly set when it hasn't been implicitly set already."
	override val suggestion = "Remove explicit parent type."
	override val suggestedAction = RemoveElement(source)
}
