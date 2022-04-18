package linter.elements.definitions

import linter.elements.general.Unit
import linter.elements.values.VariableValueDeclaration
import parsing.ast.definitions.Parameter

class Parameter(override val source: Parameter, name: String, val type: Unit?):
	VariableValueDeclaration(source, name, true) {

	init {
		if(type != null)
			units.add(type)
	}
}