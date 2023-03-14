package logger.issues.constant_conditions

import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class ExpressionNotAssignable(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Expression is not assignable."
	override val description = "The expression can only be read from and can not be assigned to."
}
