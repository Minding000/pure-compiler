package linter.elements.definitions

import linter.elements.literals.Type
import linter.elements.values.VariableValueDeclaration
import parsing.ast.definitions.Parameter

class Parameter(override val source: Parameter, name: String, type: Type?):
	VariableValueDeclaration(source, name, type, true) {

	init {
		if(type != null)
			units.add(type)
	}
}