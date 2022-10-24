package linting.semantic_model.definitions

import linting.semantic_model.types.ObjectType
import linting.semantic_model.types.StaticType
import linting.semantic_model.types.Type
import linting.semantic_model.values.VariableValueDeclaration
import linting.semantic_model.scopes.TypeScope
import parsing.tokenizer.WordAtom
import parsing.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Class(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, superType: Type?,
			val isNative: Boolean, val isMutable: Boolean):
	TypeDefinition(source, name, scope, superType) {
	private var specificDefinitions = HashMap<Map<ObjectType, Type>, Class>()
	val value = VariableValueDeclaration(source, name, StaticType(this))

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE, WordAtom.IMMUTABLE)
	}

	init {
		units.add(value)
		scope.createInstanceConstant(this)
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): Class {
		var definition = specificDefinitions[typeSubstitution]
		if(definition == null) {
			val superType = superType?.withTypeSubstitutions(typeSubstitution)
			definition = Class(source, name, scope.withTypeSubstitutions(typeSubstitution, superType?.scope), superType,
				isNative, isMutable)
			specificDefinitions[typeSubstitution] = definition
		}
		return definition
	}
}
