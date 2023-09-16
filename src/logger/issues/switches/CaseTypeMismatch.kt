package logger.issues.switches

import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class CaseTypeMismatch(source: SyntaxTreeNode, conditionType: Type, subjectType: Type): Issue(Severity.ERROR, source) {
	override val text = "Condition type '$conditionType' is not comparable to subject type '$subjectType'."
	override val description = "The type of the case value needs to be comparable to the type of the switch value."
}
