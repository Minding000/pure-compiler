package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.definitions.TypeAlias as TypeAliasSyntaxTree

class TypeAlias(override val source: TypeAliasSyntaxTree, name: String, val referenceType: Type, scope: TypeScope,
				isSpecificCopy: Boolean = false):
	TypeDefinition(source, name, scope, null, null, emptyList(), false, isSpecificCopy) {
	override val isDefinition = false

	init {
		scope.typeDefinition = this
		addUnits(referenceType)
	}

	override fun declare(linter: Linter) {
		super.declare(linter)
		scope.enclosingScope.declareType(linter, this)
	}

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): TypeAlias {
		return TypeAlias(source, name, referenceType.withTypeSubstitutions(linter, typeSubstitutions),
			scope.withTypeSubstitutions(linter, typeSubstitutions, null), true)
	}

	override fun getConversionsFrom(linter: Linter, sourceType: Type): List<InitializerDefinition> {
		return referenceType.getConversionsFrom(linter, sourceType)
	}
}
