package linter.elements.definitions

import linter.elements.general.ErrorHandlingContext
import linter.elements.general.Unit
import linter.elements.values.Value
import parsing.ast.definitions.LambdaFunctionDefinition as ASTLambdaDefinition

class LambdaFunctionDefinition(override val source: ASTLambdaDefinition, val parameters: List<Unit>,
							   val body: ErrorHandlingContext): Value(source) {

	init {
		units.addAll(parameters)
		units.add(body)
	}
}