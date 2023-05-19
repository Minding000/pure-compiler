package logger.issues.resolution

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class SelfReferenceOutsideOfTypeDefinition(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Self references are not allowed outside of type definitions."
	override val description = "Self references require a type definition to reference."
}
