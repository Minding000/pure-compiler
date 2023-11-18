package logger.issues.declaration

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class MonomorphicInheritance(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Class 'Int' cannot be inherited from."
	override val description = "Non-abstract classes containing monomorphic functions cannot be inherited from."
}
