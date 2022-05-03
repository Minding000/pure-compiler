package linter.elements.definitions

import linter.elements.general.Unit
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration
import linter.scopes.TypeScope
import parsing.ast.definitions.TypeDefinition as ASTTypeDefinition

class Object(override val source: ASTTypeDefinition, name: String, scope: TypeScope, superType: Unit?):
	TypeDefinition(source, name, scope, superType, false) {
	val value = VariableValueDeclaration(source, name, true)

	init {
		if(superType != null)
			units.add(superType)
	}
}