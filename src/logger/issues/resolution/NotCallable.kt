package logger.issues.resolution

import components.semantic_analysis.semantic_model.values.Value
import logger.Issue
import logger.Severity

class NotCallable(function: Value): Issue(Severity.ERROR, function.source) {
	override val text = "'${function.source.getValue()}' is not callable."
	override val description = "The expression can not be called, because it is not an initializer or function."
}
