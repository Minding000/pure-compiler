package linter.elements.definitions

import linter.elements.general.Unit
import linter.elements.literals.SimpleType
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration
import linter.scopes.TypeScope
import parsing.ast.definitions.TypeDefinition as ASTTypeDefinition

class Object(override val source: ASTTypeDefinition, name: String, scope: TypeScope, superType: Unit?):
	TypeDefinition(source, name, scope, superType, false) {
	val value = VariableValueDeclaration(source, name, SimpleType(this), true)

	init {
		scope.instanceConstant = value
		if(superType != null)
			units.add(superType)
	}
}