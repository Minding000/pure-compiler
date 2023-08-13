package logger.issues.declaration

import components.semantic_analysis.semantic_model.declarations.GenericTypeDeclaration
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class TypeParameterNotAssignable(source: SyntaxTreeNode, typeParameter: Type, genericType: GenericTypeDeclaration): Issue(Severity.ERROR, source) {
	override val text = "The type parameter '$typeParameter' is not assignable to '$genericType'."
	override val description = "The generic type declaration constraints are not being met by the provided type parameter."
}
