package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.LocalVariableDeclaration
import components.syntax_parser.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Enum(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, explicitParentType: ObjectType?, superType: Type?,
		   members: List<Unit>, isBound: Boolean): TypeDefinition(source, name, scope, explicitParentType, superType, members, isBound) {

	init {
		scope.typeDefinition = this
	}

	override fun declare(linter: Linter) {
		super.declare(linter)
		val targetScope = parentTypeDefinition?.scope ?: scope.enclosingScope
		targetScope.declareType(linter, this)
		val staticType = StaticType(this)
		val valueDeclaration = if(targetScope is TypeScope)
			PropertyDeclaration(source, targetScope, name, staticType, null, !isBound)
		else
			LocalVariableDeclaration(source, targetScope, name, staticType)
		addUnits(valueDeclaration)
		valueDeclaration.declare(linter)
	}

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): Enum {
		val superType = superType?.withTypeSubstitutions(linter, typeSubstitutions)
		return Enum(source, name, scope.withTypeSubstitutions(linter, typeSubstitutions, superType?.interfaceScope), explicitParentType,
			superType, members, isBound)
	}
}
