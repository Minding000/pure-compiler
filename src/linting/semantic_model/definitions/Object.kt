package linting.semantic_model.definitions

import linting.Linter
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.scopes.TypeScope
import linting.semantic_model.types.ObjectType
import linting.semantic_model.types.Type
import linting.semantic_model.values.VariableValueDeclaration
import parsing.tokenizer.WordAtom
import parsing.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Object(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, superType: Type?,
			 val isNative: Boolean, val isMutable: Boolean): TypeDefinition(source, name, scope, superType) {

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE, WordAtom.IMMUTABLE)
	}

	init {
		scope.typeDefinition = this
	}

	override fun register(linter: Linter, parentScope: MutableScope) {
		parentScope.declareType(linter, this)
		val valueDeclaration = VariableValueDeclaration(source, name, ObjectType(this))
		parentScope.declareValue(linter, valueDeclaration)
		units.add(valueDeclaration)
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): Object {
		return this
	}
}
