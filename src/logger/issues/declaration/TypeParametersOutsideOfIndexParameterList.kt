package logger.issues.declaration

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class TypeParametersOutsideOfIndexParameterList(source: SyntaxTreeNode): Issue(Severity.WARNING, source) {
	override val text = "Type parameters for the index operator are received in the index parameter list instead."
	override val description = "Type parameters for the index operator are received in the index parameter list instead."
	override val suggestion = "Move type parameters in the index parameter list."
}
