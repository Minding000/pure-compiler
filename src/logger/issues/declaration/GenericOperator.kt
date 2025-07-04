package logger.issues.declaration

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class GenericOperator(source: SyntaxTreeNode): Issue(Severity.WARNING, source) {
	override val text = "Operators (except for the index operator) can not be generic."
	override val description = "Type parameters are only allowed on index operators."
	override val suggestion = "Remove type parameters."
}
