package linter.elements.definitions

import linter.elements.literals.Type
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration
import linter.scopes.TypeScope
import parsing.ast.definitions.TypeDefinition as ASTTypeDefinition

class Enum(override val source: ASTTypeDefinition, name: String, scope: TypeScope, superType: Type?):
	TypeDefinition(source, name, scope, superType, false) {
	val value = VariableValueDeclaration(source, name, null, true) //TODO should provide type

	init {
		scope.createInstanceConstant(this)
	}
}