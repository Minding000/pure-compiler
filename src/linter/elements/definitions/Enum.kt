package linter.elements.definitions

import linter.elements.general.Unit
import linter.elements.values.TypeDefinition
import parsing.ast.definitions.TypeDefinition as ASTTypeDefinition

class Enum(val source: ASTTypeDefinition, name: String, superType: Unit?): TypeDefinition(name, superType, false) {

	init {
		if(superType != null)
			units.add(superType)
	}
}