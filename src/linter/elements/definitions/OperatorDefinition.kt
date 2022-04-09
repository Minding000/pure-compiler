package linter.elements.definitions

import linter.elements.general.Unit
import parsing.ast.definitions.OperatorDefinition

class OperatorDefinition(val source: OperatorDefinition, val parameters: List<Unit>, val body: Unit?, val returnType: Unit?):
	Unit() {

	init {
		units.addAll(parameters)
		if(body != null)
			units.add(body)
		if(returnType != null)
			units.add(returnType)
	}
}