package linter.elements.definitions

import linter.elements.general.Unit
import parsing.ast.definitions.InitializerDefinition

class InitializerDefinition(val source: InitializerDefinition, val parameters: List<Unit>, val body: Unit?): Unit() {

	init {
		units.addAll(parameters)
		if(body != null)
			units.add(body)
	}
}