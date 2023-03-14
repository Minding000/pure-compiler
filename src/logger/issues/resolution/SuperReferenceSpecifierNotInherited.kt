package logger.issues.resolution

import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.types.ObjectType
import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class SuperReferenceSpecifierNotInherited(source: Element, surroundingDefinition: TypeDefinition, specifier: ObjectType):
	Issue(Severity.ERROR, source) {
	override val text = "'${surroundingDefinition.name}' does not inherit from '$specifier'."
	override val description = "Super reference specifiers need to be a super class of the class that surrounds them."
}
