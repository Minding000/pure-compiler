package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.LocalVariableDeclaration
import components.syntax_parser.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Object(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, explicitParentType: ObjectType?,
			 superType: Type?, members: List<Unit>, isBound: Boolean, val isNative: Boolean, val isMutable: Boolean):
	TypeDefinition(source, name, scope, explicitParentType, superType, members, isBound) {

	init {
		scope.typeDefinition = this
	}

	override fun declare(linter: Linter) {
		super.declare(linter)
		val targetScope = parentTypeDefinition?.scope ?: scope.enclosingScope
		targetScope.declareType(linter, this)
		val type = ObjectType(this)
		val valueDeclaration = if(targetScope is TypeScope)
			PropertyDeclaration(source, targetScope, name, type, null, !isBound)
		else
			LocalVariableDeclaration(source, targetScope, name, type)
		addUnits(valueDeclaration)
		valueDeclaration.declare(linter)
	}

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): Object {
		return this //TODO What about bound objects?
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		scope.ensureTrivialInitializers(linter)
	}
}
