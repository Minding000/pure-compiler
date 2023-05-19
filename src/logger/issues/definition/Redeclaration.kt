package logger.issues.definition

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class Redeclaration(source: SyntaxTreeNode, kind: String, signature: String, originalSource: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Redeclaration of $kind '$signature', previously declared in ${originalSource.getStartString()}."
	override val description = "This element has already been declared and can therefore not be declared again."
}
