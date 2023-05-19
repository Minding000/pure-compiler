package logger.issues.loops

import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.LocalVariableDeclaration
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class TooManyIterableVariableDeclarations(source: SyntaxTreeNode, variableDeclarations: List<LocalVariableDeclaration>,
										  availableValueTypes: List<Type?>): Issue(Severity.ERROR, source) {
	override val text = "The number of declared variables (${variableDeclarations.size}) is larger than the number of values" +
		" provided by the iterables iterator (${availableValueTypes.size})."
	override val description = "Iterables can have a different number of provided values depending on which classes they implement."
}
