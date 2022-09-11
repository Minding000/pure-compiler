package linter.elements.definitions

import linter.elements.literals.ObjectType
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
	private val specificDefinitions = HashMap<Map<ObjectType, Type>, Class>()
	val value = VariableValueDeclaration(source, name, StaticType(this), null, true)

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE)
	}

	init {
		units.add(value)
		scope.createInstanceConstant(this)
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): Class {
		var definition = specificDefinitions[typeSubstitution]
		if(definition == null) {
			val superType = superType?.withTypeSubstitutions(typeSubstitution)
			definition = Class(source, name, scope.withTypeSubstitutions(typeSubstitution, superType?.scope), superType, isNative)
			specificDefinitions[typeSubstitution] = definition
		}
		return definition
	}
}