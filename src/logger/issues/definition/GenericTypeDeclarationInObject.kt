package logger.issues.definition

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity
import logger.actions.RemoveElement

class GenericTypeDeclarationInObject(source: SyntaxTreeNode): Issue(Severity.WARNING, source) {
	override val text = "Generic type declarations are not allowed in objects."
	override val description = "Objects can not be generic, because they are already instantiated."
	override val suggestion = "Remove generic type declaration."
	override val suggestedAction = RemoveElement(source)
}
