package logger.issues.definition

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class PropertyParameterOutsideOfInitializer(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Property parameter is not allowed here."
	override val description = "Property parameters are only allowed in initializers."
	override val suggestion = "Add a type."
}
