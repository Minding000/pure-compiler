package logger.issues.declaration

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class DisallowedDeclarationType(source: SyntaxTreeNode, kind: String, context: String): Issue(Severity.ERROR, source) {
	override val text = "$kind declarations aren't allowed in '$context'."
	override val description = "This element is not allowed in the given context."
}
