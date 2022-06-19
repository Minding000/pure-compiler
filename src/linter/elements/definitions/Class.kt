package linter.elements.definitions

import linter.Linter
import linter.elements.general.Unit
import linter.elements.values.TypeDefinition
import linter.scopes.Scope
import linter.scopes.TypeScope
import parsing.tokenizer.WordAtom
import parsing.ast.definitions.TypeDefinition as ASTTypeDefinition

class Class(override val source: ASTTypeDefinition, name: String, scope: TypeScope, superType: Unit?,
			val isNative: Boolean):
	TypeDefinition(source, name, scope, superType, false) {

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE)
	}

	init {
		scope.createInstanceConstant(this)
	}
}