package logger.issues.initialization

import components.semantic_model.declarations.PropertyDeclaration
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class ReliesOnUninitializedProperties(source: SyntaxTreeNode, signature: String,
									  requiredButUninitializedProperties: List<PropertyDeclaration>): Issue(Severity.ERROR, source) {
	override val text = "The callable '$signature' relies on the following uninitialized properties:" +
		requiredButUninitializedProperties.joinToString("") { property -> "\n - $property" }
	override val description = "This callable relies on properties that might be uninitialized when it is called."
	override val suggestion = "Initialize the properties before making this call or remove the dependency on the properties."
}
