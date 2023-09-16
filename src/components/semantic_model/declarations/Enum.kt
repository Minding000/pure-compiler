package components.semantic_model.declarations

import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.TypeScope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import components.semantic_model.values.LocalVariableDeclaration
import components.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Enum(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, explicitParentType: ObjectType?, superType: Type?,
		   members: List<SemanticModel>, isBound: Boolean):
	TypeDeclaration(source, name, scope, explicitParentType, superType, members, isBound) {

	init {
		scope.typeDeclaration = this
	}

	override fun getValueDeclaration(): ValueDeclaration {
		val targetScope = parentTypeDeclaration?.scope ?: scope.enclosingScope
		val staticType = StaticType(this)
		staticValueDeclaration = if(targetScope is TypeScope)
			PropertyDeclaration(source, targetScope, name, staticType, null, !isBound)
		else
			LocalVariableDeclaration(source, targetScope, name, staticType)
		return staticValueDeclaration
	}

	override fun declare() {
		super.declare()
		val targetScope = parentTypeDeclaration?.scope ?: scope.enclosingScope
		targetScope.addTypeDeclaration(this)
	}
}
