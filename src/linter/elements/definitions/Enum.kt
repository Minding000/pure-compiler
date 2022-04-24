package linter.elements.definitions

import linter.elements.general.Unit
import linter.elements.values.TypeDefinition
import parsing.ast.definitions.TypeDefinition as ASTTypeDefinition

class Enum(override val source: ASTTypeDefinition, name: String, superType: Unit?):
	TypeDefinition(source, name, superType, false) {

	init {
		if(superType != null)
			units.add(superType)
	}
}