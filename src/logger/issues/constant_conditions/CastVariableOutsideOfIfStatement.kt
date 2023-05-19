package logger.issues.constant_conditions

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class CastVariableOutsideOfIfStatement(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "'is' casts can only declare a variable in an if statement condition."
	override val description = "The declared variable is only valid if the cast succeed, so an if statement is required."
}
