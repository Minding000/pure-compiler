package linting.semantic_model.definitions

import linting.semantic_model.types.ObjectType
import linting.semantic_model.types.Type
import linting.semantic_model.scopes.TypeScope
import parsing.syntax_tree.definitions.Parameter

class GenericTypeDefinition(override val source: Parameter, name: String, scope: TypeScope, superType: Type?):
	TypeDefinition(source, name, scope, superType) {
	private val specificDefinitions = HashMap<Map<ObjectType, Type>, GenericTypeDefinition>()

	override fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): GenericTypeDefinition {
		var definition = specificDefinitions[typeSubstitution]
		if(definition == null) {
			val superType = superType?.withTypeSubstitutions(typeSubstitution)
			definition = GenericTypeDefinition(source, name,
				scope.withTypeSubstitutions(typeSubstitution, superType?.scope), superType)
			specificDefinitions[typeSubstitution] = definition
		}
		return definition
	}
}
