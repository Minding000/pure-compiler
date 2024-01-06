package components.semantic_model.declarations

import components.semantic_model.scopes.TypeScope
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import kotlin.properties.Delegates
import components.syntax_parser.syntax_tree.definitions.Parameter as ParameterSyntaxTree

class GenericTypeDeclaration(override val source: ParameterSyntaxTree, name: String, scope: TypeScope, superType: Type?):
	TypeDeclaration(source, name, scope, null, superType) {
	override val isDefinition = false
	var index by Delegates.notNull<Int>()

	init {
		scope.typeDeclaration = this
	}

	override fun getValueDeclaration(): ValueDeclaration {
		val targetScope = parentTypeDeclaration?.scope ?: scope.enclosingScope
		val staticType = StaticType(this)
		staticValueDeclaration = if(targetScope is TypeScope)
			PropertyDeclaration(source, targetScope, name, staticType)
		else
			GlobalValueDeclaration(source, targetScope, name, staticType)
		return staticValueDeclaration
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
