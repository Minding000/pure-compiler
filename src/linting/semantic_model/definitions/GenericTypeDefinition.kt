package linting.semantic_model.definitions

import linting.semantic_model.scopes.TypeScope
import linting.semantic_model.types.Type
import components.parsing.syntax_tree.definitions.Parameter as ParameterSyntaxTree

class GenericTypeDefinition(override val source: ParameterSyntaxTree, name: String, scope: TypeScope, superType: Type?):
	TypeDefinition(source, name, scope, superType) {
	private val specificDefinitions = HashMap<Map<TypeDefinition, Type>, GenericTypeDefinition>()

	init {
		scope.typeDefinition = this
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<TypeDefinition, Type>): GenericTypeDefinition {
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
