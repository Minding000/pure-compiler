package logger.issues.declaration

import components.semantic_model.declarations.FunctionImplementation
import logger.Issue
import logger.Severity
import util.uppercaseFirstChar

class MissingNativeImplementation(functionImplementation: FunctionImplementation): Issue(Severity.ERROR, functionImplementation.source) {
	override val text = "${functionImplementation.memberType.uppercaseFirstChar()} '${functionImplementation}' is missing a native implementation."
	override val description = "A native implementation is required."
	override val suggestion = "Add the missing native implementation."
}
