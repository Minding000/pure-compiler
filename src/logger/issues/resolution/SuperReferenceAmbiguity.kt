package logger.issues.resolution

import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class SuperReferenceAmbiguity(source: SyntaxTreeNode, possibleTargetTypes: List<Type>):
	Issue(Severity.ERROR, source) {
	override val text = "The super reference is ambiguous. Possible targets are:" + possibleTargetTypes.joinToString("") { "\n - $it" }
	override val description = "The super member exists, but there are multiple types with that member."
}
