package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.definitions.TypeAlias as TypeAliasSyntaxTree

class TypeAlias(override val source: TypeAliasSyntaxTree, name: String, val referenceType: Type, scope: TypeScope):
	TypeDefinition(source, name, scope, null) {
	private val specificDefinitions = HashMap<Map<TypeDefinition, Type>, TypeAlias>()

	init {
		scope.typeDefinition = this
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<TypeDefinition, Type>): TypeAlias {
		var definition = specificDefinitions[typeSubstitution]
		if(definition == null) {
			definition = TypeAlias(source, name, referenceType.withTypeSubstitutions(typeSubstitution),
				scope.withTypeSubstitutions(typeSubstitution, null))
			specificDefinitions[typeSubstitution] = definition
		}
		return definition
	}
}
