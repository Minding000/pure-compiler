package linter.elements.definitions

import linter.elements.general.Unit
import linter.elements.values.TypeDefinition
import linter.scopes.TypeScope
import parsing.ast.definitions.TypeDefinition as ASTTypeDefinition

class Class(override val source: ASTTypeDefinition, name: String, scope: TypeScope, superType: Unit?):
	TypeDefinition(source, name, scope, superType, false) {

	init {
		scope.createInstanceConstant(this)
		if(superType != null)
			units.add(superType)
	}
}