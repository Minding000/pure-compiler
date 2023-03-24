package logger.issues.modifiers

import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration
import logger.Issue
import logger.Severity

class VariablePropertyOverriddenByValue(propertyDeclaration: PropertyDeclaration): Issue(Severity.ERROR, propertyDeclaration.source) {
	override val text = "Variable super property '${propertyDeclaration.name}' cannot be overridden by value property."
	override val description = "The variable super property cannot be overridden by a value property, because it might be written to."
	override val suggestion = "Make property variable."
}
