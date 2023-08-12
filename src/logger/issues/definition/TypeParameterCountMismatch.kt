package logger.issues.definition

import components.semantic_analysis.semantic_model.definitions.GenericTypeDeclaration
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class TypeParameterCountMismatch(source: SyntaxTreeNode, typeParameters: List<Type>, genericTypes: List<GenericTypeDeclaration>):
	Issue(Severity.ERROR, source) {
	override val text = "Number of provided type parameters (${typeParameters.size})" +
		" doesn't match number of declared generic types (${genericTypes.size})."
	override val description = "Computed value properties cannot have setters, because they cannot be reassigned."
}
