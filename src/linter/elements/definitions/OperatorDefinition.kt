package linter.elements.definitions

import linter.elements.general.Unit
import linter.elements.values.VariableValueDeclaration
import parsing.ast.definitions.OperatorDefinition

class OperatorDefinition(override val source: OperatorDefinition, name: String, val parameters: List<Parameter>,
						  val body: Unit?, val returnType: Unit?):
	VariableValueDeclaration(source, name, true) {
	val variation = parameters.joinToString { parameter -> "${parameter.name}-${parameter.type}"}

	init {
		units.addAll(parameters)
		if(body != null)
			units.add(body)
		if(returnType != null)
			units.add(returnType)
	}
}