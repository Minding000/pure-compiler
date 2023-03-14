package logger.issues.initialization

import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration
import logger.Issue
import logger.Severity

class CircularAssignment(propertyDeclaration: PropertyDeclaration): Issue(Severity.ERROR, propertyDeclaration.source) {
	override val text = "'${propertyDeclaration.name}' has no value, because it's part of a circular assignment."
	override val description = "The value used to initialize this property depends on the value itself."
}
