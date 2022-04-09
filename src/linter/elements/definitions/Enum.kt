package linter.elements.definitions

import linter.elements.general.Unit
import parsing.ast.definitions.TypeDefinition

class Enum(val source: TypeDefinition, val name: String, val superType: Unit?): Unit() {

	init {
		if(superType != null)
			units.add(superType)
	}
}