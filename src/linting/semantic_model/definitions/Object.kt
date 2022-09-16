package linting.semantic_model.definitions

import linting.semantic_model.literals.ObjectType
import linting.semantic_model.literals.Type
import linting.semantic_model.scopes.TypeScope
import linting.semantic_model.values.TypeDefinition
import linting.semantic_model.values.VariableValueDeclaration
import parsing.tokenizer.WordAtom
import parsing.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Object(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, superType: Type?):
	TypeDefinition(source, name, scope, superType) {
	val value = VariableValueDeclaration(source, name, ObjectType(this), null, true)

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE)
	}

	init {
		units.add(value)
		scope.instanceConstant = value
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): Object {
		return this
	}
}