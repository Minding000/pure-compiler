package logger.issues.declaration

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class DeclarationMissingTypeOrValue(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Declaration requires a type or a value to infer a type from."
	override val description = "Declarations require either a type specified directly or a value to infer a type from."
	override val suggestion = "Add a type or a value."
}
