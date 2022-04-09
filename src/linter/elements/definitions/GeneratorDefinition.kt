package linter.elements.definitions

import linter.elements.general.Unit
import linter.elements.values.VariableValueDeclaration
import parsing.ast.definitions.GeneratorDefinition

class GeneratorDefinition(override val source: GeneratorDefinition, name: String, val parameters: List<Unit>,
						  val keyReturnType: Unit?, val valueReturnType: Unit, val body: Unit):
	VariableValueDeclaration(source, name) {

	init {
		units.addAll(parameters)
		if(keyReturnType != null)
			units.add(keyReturnType)
		units.add(valueReturnType)
		units.add(body)
	}
}