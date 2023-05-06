package logger.issues.resolution

import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class SelfReferenceSpecifierNotBound(source: Element, surroundingDefinition: TypeDefinition, specifierDefinition: TypeDefinition):
	Issue(Severity.ERROR, source) {
	override val text = "Specified type '${specifierDefinition.name}' is not bound to" +
		" type '${surroundingDefinition.name}' surrounding the self reference."
	override val description = "Self references need to be bound to the type they specify."
}
