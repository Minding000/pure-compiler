package linter.elements.definitions

import linter.elements.literals.Type
import linter.elements.values.TypeDefinition
import linter.scopes.TypeScope
import parsing.tokenizer.WordAtom
import parsing.ast.definitions.TypeDefinition as ASTTypeDefinition

class Class(override val source: ASTTypeDefinition, name: String, scope: TypeScope, superType: Type?,
			val isNative: Boolean):
	TypeDefinition(source, name, scope, superType, false) {

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE)
	}

	init {
		scope.createInstanceConstant(this)
	}
}