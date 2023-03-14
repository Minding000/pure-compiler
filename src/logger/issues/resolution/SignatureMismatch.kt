package logger.issues.resolution

import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import logger.Issue
import logger.Severity
import util.stringifyTypes

class SignatureMismatch(val function: Value, val typeParameters: List<Type>, val valueParameters: List<Value>):
	Issue(Severity.ERROR, function.source) {
	override val text: String
		get() {
			var parameterStringRepresentation = ""
			if(typeParameters.isNotEmpty()) {
				parameterStringRepresentation += typeParameters.joinToString()
				parameterStringRepresentation += ";"
				if(valueParameters.isNotEmpty())
					parameterStringRepresentation += " "
			}
			parameterStringRepresentation += valueParameters.stringifyTypes()
			return "The provided parameters ($parameterStringRepresentation) don't match any signature " +
				"of function '${function.source.getValue()}'."
		}
	override val description =
		"The callable exists, but there is no overload with parameters that accept the provided types and values."
}
