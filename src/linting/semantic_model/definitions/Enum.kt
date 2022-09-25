package linting.semantic_model.definitions

import linting.semantic_model.literals.ObjectType
import linting.semantic_model.literals.Type
import linting.semantic_model.values.VariableValueDeclaration
import linting.semantic_model.scopes.TypeScope
import parsing.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Enum(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, superType: Type?):
	TypeDefinition(source, name, scope, superType) {
	private val specificDefinitions = HashMap<Map<ObjectType, Type>, Enum>()
	val value = VariableValueDeclaration(source, name, null, null, true) //TODO should provide type

	init {
		scope.createInstanceConstant(this)
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): Enum {
		var definition = specificDefinitions[typeSubstitution]
		if(definition == null) {
			val superType = superType?.withTypeSubstitutions(typeSubstitution)
			definition = Enum(source, name, scope.withTypeSubstitutions(typeSubstitution, superType?.scope), superType)
			specificDefinitions[typeSubstitution] = definition
		}
		return definition
	}
}