package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.definitions.TypeAlias as TypeAliasSyntaxTree

class TypeAlias(override val source: TypeAliasSyntaxTree, name: String, val referenceType: Type, scope: TypeScope):
	TypeDefinition(source, name, scope, null) {

	init {
		scope.typeDefinition = this
		addUnits(referenceType)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): TypeAlias {
		return TypeAlias(source, name, referenceType.withTypeSubstitutions(typeSubstitutions),
			scope.withTypeSubstitutions(typeSubstitutions, null))
	}
}
