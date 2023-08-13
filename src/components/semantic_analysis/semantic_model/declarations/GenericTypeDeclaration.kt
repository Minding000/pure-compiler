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

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): GenericTypeDeclaration {
		val superType = superType?.withTypeSubstitutions(typeSubstitutions)
		return GenericTypeDeclaration(source, name, scope.withTypeSubstitutions(typeSubstitutions, superType?.interfaceScope), superType)
	}

	override fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		return getLinkedSuperType()?.getConversionsFrom(sourceType) ?: listOf()
	}
}
