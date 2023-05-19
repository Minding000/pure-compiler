package logger.issues.definition

import components.semantic_analysis.semantic_model.definitions.GenericTypeDefinition
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class TypeParameterNotAssignable(source: SyntaxTreeNode, typeParameter: Type, genericType: GenericTypeDefinition): Issue(Severity.ERROR, source) {
	override val text = "The type parameter '$typeParameter' is not assignable to '$genericType'."
	override val description = "The generic type definitions constraints are not being met by the provided type parameter."
}
