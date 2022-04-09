package linter.elements.definitions

import linter.elements.general.ErrorHandlingContext
import linter.elements.general.Unit
import parsing.ast.definitions.LambdaFunctionDefinition as ASTLambdaDefinition

class LambdaFunctionDefinition(val source: ASTLambdaDefinition, val parameters: List<Unit>, val body: ErrorHandlingContext): Unit() {

	init {
		units.addAll(parameters)
		units.add(body)
	}
}