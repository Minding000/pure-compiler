package logger.issues.initialization

import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration
import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class ReliesOnUninitializedProperties(source: Element, signature: String, requiredButUninitializedProperties: List<PropertyDeclaration>):
	Issue(Severity.ERROR, source) {
	override val text = "The callable '$signature' relies on the following uninitialized properties:" +
		requiredButUninitializedProperties.joinToString("") { "\n - ${it.memberIdentifier}" }
	override val description = "This callable relies on properties that might be uninitialized when it is called."
	override val suggestion = "Initialize the properties before making this call or remove the dependency on the properties."
}
