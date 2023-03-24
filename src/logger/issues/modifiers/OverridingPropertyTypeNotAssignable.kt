package logger.issues.modifiers

import components.semantic_analysis.semantic_model.types.Type
import logger.Issue
import logger.Severity

class OverridingPropertyTypeNotAssignable(overridingType: Type, superType: Type): Issue(Severity.ERROR, overridingType.source) {
	override val text = "Type of overriding property '$overridingType' is not assignable to" +
		" the type of the overridden property '$superType'."
	override val description = "The type of the overriding property is not assignable to the type of the overridden property."
	override val suggestion = "Change type to be assignable to the super type."
}
