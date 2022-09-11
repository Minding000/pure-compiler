package linting.semantic_model.definitions

import linting.semantic_model.literals.ObjectType
import linting.semantic_model.literals.Type
import linting.semantic_model.values.TypeDefinition
import linting.semantic_model.scopes.TypeScope
import parsing.syntax_tree.definitions.TypeAlias as TypeAliasSyntaxTree

class TypeAlias(override val source: TypeAliasSyntaxTree, name: String, val referenceType: Type, scope: TypeScope):
	TypeDefinition(source, name, scope, null) {
	private val specificDefinitions = HashMap<Map<ObjectType, Type>, TypeAlias>()

	override fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): TypeAlias {
		var definition = specificDefinitions[typeSubstitution]
		if(definition == null) {
			definition = TypeAlias(source, name, referenceType.withTypeSubstitutions(typeSubstitution),
				scope.withTypeSubstitutions(typeSubstitution, null))
			specificDefinitions[typeSubstitution] = definition
		}
		return definition
	}
}