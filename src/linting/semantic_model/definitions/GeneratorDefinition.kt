package linting.semantic_model.definitions

import linting.semantic_model.general.Unit
import linting.semantic_model.values.VariableValueDeclaration
import parsing.syntax_tree.definitions.GeneratorDefinition

class GeneratorDefinition(override val source: GeneratorDefinition, name: String, val parameters: List<Unit>,
						  val keyReturnType: Unit?, val valueReturnType: Unit, val body: Unit):
	VariableValueDeclaration(source, name, null, null, true) {

	init {
		units.addAll(parameters)
		if(keyReturnType != null)
			units.add(keyReturnType)
		units.add(valueReturnType)
		units.add(body)
	}
}