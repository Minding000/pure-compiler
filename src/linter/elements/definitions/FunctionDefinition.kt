package linter.elements.definitions

import linter.elements.general.Unit
import linter.elements.values.VariableValueDeclaration
import parsing.ast.definitions.FunctionDefinition

class FunctionDefinition(override val source: FunctionDefinition, name: String, val genericParameters: List<Unit>,
						 val parameters: List<Parameter>, val body: Unit?, val returnType: Unit?, val isNative: Boolean):
	VariableValueDeclaration(source, name, true) {
	val variation = parameters.joinToString { parameter -> "${parameter.name}-${parameter.type}"}

	init {
		units.addAll(genericParameters)
		units.addAll(parameters)
		if(body != null)
			units.add(body)
		if(returnType != null)
			units.add(returnType)
	}
}