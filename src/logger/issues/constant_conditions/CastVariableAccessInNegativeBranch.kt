package logger.issues.constant_conditions

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class CastVariableAccessInNegativeBranch(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Cannot access cast variable in negative branch."
	override val description = "The cast variable is only available in the branch where the cast succeeded."
}
