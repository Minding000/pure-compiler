package components.semantic_model.declarations

import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.TypeScope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Class(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, explicitParentType: ObjectType?,
			superType: Type?, members: List<SemanticModel>, val isAbstract: Boolean, isBound: Boolean, val isCopied: Boolean,
			val isNative: Boolean, val isMutable: Boolean):
	TypeDeclaration(source, name, scope, explicitParentType, superType, members, isBound) {

	init {
		scope.typeDeclaration = this
	}

	override fun getValueDeclaration(): ValueDeclaration {
		val targetScope = parentTypeDeclaration?.scope ?: scope.enclosingScope
		val staticType = StaticType(this)
		staticValueDeclaration = if(targetScope is TypeScope)
			PropertyDeclaration(source, targetScope, name, staticType, null, !isBound, isAbstract)
		else
			GlobalValueDeclaration(source, targetScope, name, staticType)
		return staticValueDeclaration
	}

	override fun declare() {
		super.declare()
		val targetScope = parentTypeDeclaration?.scope ?: scope.enclosingScope
		targetScope.addTypeDeclaration(this)
	}

	override fun validate() {
		super.validate()
		if(!isAbstract)
			scope.ensureNoAbstractMembers()
	}

	fun containsMonomorphicMemberImplementation(): Boolean {
		return scope.memberDeclarations.any { memberDeclaration ->
			memberDeclaration is FunctionImplementation && memberDeclaration.isMonomorphic && !memberDeclaration.isAbstract }
	}
}
