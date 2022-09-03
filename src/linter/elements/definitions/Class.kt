package linter.elements.definitions

import linter.elements.literals.StaticType
import linter.elements.literals.Type
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration
import linter.scopes.TypeScope
import parsing.tokenizer.WordAtom
import parsing.ast.definitions.TypeDefinition as ASTTypeDefinition

class Class(override val source: ASTTypeDefinition, name: String, scope: TypeScope, superType: Type?,
			val isNative: Boolean):
	TypeDefinition(source, name, scope, superType) {
	val value = VariableValueDeclaration(source, name, StaticType(this), null, true)

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE)
	}

	init {
		units.add(value)
		scope.createInstanceConstant(this)
	}
}