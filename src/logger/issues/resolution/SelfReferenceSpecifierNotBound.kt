package logger.issues.resolution

import components.semantic_analysis.semantic_model.definitions.TypeDeclaration
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class SelfReferenceSpecifierNotBound(source: SyntaxTreeNode, surroundingDeclaration: TypeDeclaration, specifierDefinition: TypeDeclaration):
	Issue(Severity.ERROR, source) {
	override val text = "Specified type '${specifierDefinition.name}' is not bound to" +
		" type '${surroundingDeclaration.name}' surrounding the self reference."
	override val description = "Self references need to be bound to the type they specify."
}
