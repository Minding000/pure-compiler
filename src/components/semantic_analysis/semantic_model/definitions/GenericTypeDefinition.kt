package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.definitions.Parameter as ParameterSyntaxTree

class GenericTypeDefinition(override val source: ParameterSyntaxTree, name: String, scope: TypeScope, superType: Type?):
	TypeDefinition(source, name, scope, superType) {

	init {
		scope.typeDefinition = this
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): GenericTypeDefinition {
		val superType = superType?.withTypeSubstitutions(typeSubstitutions)
		return GenericTypeDefinition(source, name,
			scope.withTypeSubstitutions(typeSubstitutions, superType?.scope), superType)
	}
}
