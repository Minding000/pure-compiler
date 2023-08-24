package components.semantic_analysis.semantic_model.declarations

import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.definitions.Parameter as ParameterSyntaxTree

class GenericTypeDeclaration(override val source: ParameterSyntaxTree, name: String, scope: TypeScope, superType: Type?):
	TypeDeclaration(source, name, scope, null, superType) {
	override val isDefinition = false

	init {
		scope.typeDeclaration = this
	}

	override fun declare() {
		super.declare()
		scope.enclosingScope.addTypeDeclaration(this)
	}

	fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): GenericTypeDeclaration {
		return GenericTypeDeclaration(source, name, scope, superType?.withTypeSubstitutions(typeSubstitutions))
	}

	override fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		return getLinkedSuperType()?.getConversionsFrom(sourceType) ?: emptyList()
	}
}
