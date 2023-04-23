package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.definitions.Parameter as ParameterSyntaxTree

class GenericTypeDefinition(override val source: ParameterSyntaxTree, name: String, scope: TypeScope, superType: Type?):
	TypeDefinition(source, name, scope, null, superType) {
	override val isDefinition = false

	init {
		scope.typeDefinition = this
	}

	override fun declare(linter: Linter) {
		super.declare(linter)
		scope.enclosingScope.declareType(linter, this)
	}

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): GenericTypeDefinition {
		val superType = superType?.withTypeSubstitutions(linter, typeSubstitutions)
		return GenericTypeDefinition(source, name, scope.withTypeSubstitutions(linter, typeSubstitutions, superType?.interfaceScope),
			superType)
	}

	override fun getConversionsFrom(linter: Linter, sourceType: Type): List<InitializerDefinition> {
		return superType?.getConversionsFrom(linter, sourceType) ?: listOf()
	}
}
