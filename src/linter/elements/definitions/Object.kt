package linter.elements.definitions

import linter.elements.general.Unit
import linter.elements.values.VariableValueDeclaration
import parsing.ast.definitions.TypeDefinition

class Object(override val source: TypeDefinition, name: String, val superType: Unit?):
	VariableValueDeclaration(source, name, true) {

	init {
		if(superType != null)
			units.add(superType)
	}
}