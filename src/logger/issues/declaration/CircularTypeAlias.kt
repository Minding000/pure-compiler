package logger.issues.declaration

import components.semantic_model.declarations.TypeAlias
import logger.Issue
import logger.Severity

class CircularTypeAlias(typeAlias: TypeAlias): Issue(Severity.ERROR, typeAlias.source) {
	override val text = "'${typeAlias.name}' has no type, because it's part of a circular assignment."
	override val description = "The type used to initialize this property depends on the type itself."
}
