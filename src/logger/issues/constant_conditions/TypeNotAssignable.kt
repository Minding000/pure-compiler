package logger.issues.constant_conditions

import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class TypeNotAssignable(source: Element, sourceType: Type, targetType: Type): Issue(Severity.ERROR, source) {
	override val text = "Type '$sourceType' is not assignable to type '$targetType'."
	override val description = "The source type doesn't match the target type."
}
