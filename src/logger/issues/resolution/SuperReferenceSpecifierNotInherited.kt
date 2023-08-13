package logger.issues.resolution

import components.semantic_analysis.semantic_model.declarations.TypeDeclaration
import components.semantic_analysis.semantic_model.types.ObjectType
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class SuperReferenceSpecifierNotInherited(source: SyntaxTreeNode, surroundingTypeDeclaration: TypeDeclaration, specifier: ObjectType):
	Issue(Severity.ERROR, source) {
	override val text = "'${surroundingTypeDeclaration.name}' does not inherit from '$specifier'."
	override val description = "Super reference specifiers need to be a super class of the class that surrounds them."
}
