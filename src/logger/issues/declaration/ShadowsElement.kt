package logger.issues.declaration

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class ShadowsElement(source: SyntaxTreeNode, kind: String, name: String, previousSource: SyntaxTreeNode): Issue(Severity.WARNING, source) {
	override val text = "'$name' shadows a $kind, previously declared in ${previousSource.getStartString()}."
	override val description = "The element shadows another element with the same name."
}
