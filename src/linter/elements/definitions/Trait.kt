package linter.elements.definitions

import linter.elements.literals.Type
import linter.elements.values.TypeDefinition
import linter.scopes.TypeScope
import parsing.ast.definitions.TypeDefinition as ASTTypeDefinition

class Trait(override val source: ASTTypeDefinition, name: String, scope: TypeScope, superType: Type?):
	TypeDefinition(source, name, scope, superType, false) {

	init {
		scope.createInstanceConstant(this)
	}
}