package components.semantic_model.declarations

import components.semantic_model.general.SemanticModel
import components.semantic_model.operations.FunctionCall
import components.semantic_model.scopes.TypeScope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import components.semantic_model.values.LocalVariableDeclaration
import components.semantic_model.values.Value
import components.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Object(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, explicitParentType: ObjectType?,
			 superType: Type?, members: List<SemanticModel>, isBound: Boolean, val isNative: Boolean, val isMutable: Boolean):
	TypeDeclaration(source, name, scope, explicitParentType, superType, members, isBound) {

	init {
		scope.typeDeclaration = this
	}

	override fun getValueDeclaration(): ValueDeclaration {
		val targetScope = parentTypeDeclaration?.scope ?: scope.enclosingScope
		val staticType = StaticType(this)
		val value = FunctionCall(source, scope, Value(source, scope, staticType))
		val type = ObjectType(this)
		return if(targetScope is TypeScope)
			PropertyDeclaration(source, targetScope, name, type, value, !isBound)
		else
			LocalVariableDeclaration(source, targetScope, name, type, value)
	}

	override fun declare() {
		super.declare()
		val targetScope = parentTypeDeclaration?.scope ?: scope.enclosingScope
		targetScope.addTypeDeclaration(this)
	}

	override fun validate() {
		super.validate()
		scope.ensureTrivialInitializers()
		scope.ensureNoAbstractMembers()
	}
}
