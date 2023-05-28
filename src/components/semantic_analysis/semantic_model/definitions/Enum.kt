package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.LocalVariableDeclaration
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Enum(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, explicitParentType: ObjectType?, superType: Type?,
		   members: List<SemanticModel>, isBound: Boolean, isSpecificCopy: Boolean = false):
	TypeDefinition(source, name, scope, explicitParentType, superType, members, isBound, isSpecificCopy) {

	init {
		scope.typeDefinition = this
	}

	override fun getValueDeclaration(): ValueDeclaration {
		val targetScope = parentTypeDefinition?.scope ?: scope.enclosingScope
		val staticType = StaticType(this)
		return if(targetScope is TypeScope)
			PropertyDeclaration(source, targetScope, name, staticType, null, !isBound)
		else
			LocalVariableDeclaration(source, targetScope, name, staticType)
	}

	override fun declare() {
		super.declare()
		val targetScope = parentTypeDefinition?.scope ?: scope.enclosingScope
		targetScope.declareType(this)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Enum {
		determineTypes()
		val superType = superType?.withTypeSubstitutions(typeSubstitutions)
		return Enum(source, name, scope.withTypeSubstitutions(typeSubstitutions, superType?.interfaceScope), explicitParentType,
			superType, members, isBound, true)
	}
}
