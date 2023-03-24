package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.definitions.TypeAlias as TypeAliasSyntaxTree

class TypeAlias(override val source: TypeAliasSyntaxTree, name: String, val referenceType: Type, scope: TypeScope):
	TypeDefinition(source, name, scope, null, null) {
	override val isDefinition = false

	init {
		scope.typeDefinition = this
		addUnits(referenceType)
	}

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): TypeAlias {
		return TypeAlias(source, name, referenceType.withTypeSubstitutions(linter, typeSubstitutions),
			scope.withTypeSubstitutions(linter, typeSubstitutions, null))
	}

	override fun getConversionsFrom(linter: Linter, sourceType: Type): List<InitializerDefinition> {
		return referenceType.getConversionsFrom(linter, sourceType)
	}
}
