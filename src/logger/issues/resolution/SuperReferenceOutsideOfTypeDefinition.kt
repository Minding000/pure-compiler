package logger.issues.resolution

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class SuperReferenceOutsideOfTypeDefinition(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Super references are not allowed outside of type definitions."
	override val description = "Super references require a type definition to reference."
}
