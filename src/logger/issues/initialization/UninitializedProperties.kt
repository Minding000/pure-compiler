package logger.issues.initialization

import components.semantic_model.declarations.PropertyDeclaration
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class UninitializedProperties(source: SyntaxTreeNode, propertiesToBeInitialized: List<PropertyDeclaration>): Issue(Severity.ERROR, source) {
	override val text = "The following properties have not been initialized by this initializer:" +
		propertiesToBeInitialized.joinToString("") { property -> "\n - $property" }
	override val description = "Initializers have to initialize all properties."
	override val suggestion = "Initialize uninitialized properties."
}
