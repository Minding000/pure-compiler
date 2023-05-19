package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.LocalVariableDeclaration
import components.syntax_parser.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Class(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, explicitParentType: ObjectType?,
			superType: Type?, members: List<Unit>, val isAbstract: Boolean, isBound: Boolean, val isNative: Boolean,
			val isMutable: Boolean, isSpecificCopy: Boolean = false):
	TypeDefinition(source, name, scope, explicitParentType, superType, members, isBound, isSpecificCopy) {

	init {
		scope.typeDefinition = this
	}

	override fun declare() {
		super.declare()
		val targetScope = parentTypeDefinition?.scope ?: scope.enclosingScope
		targetScope.declareType(this)
		val staticType = StaticType(this)
		val valueDeclaration = if(targetScope is TypeScope)
			PropertyDeclaration(source, targetScope, name, staticType, null, !isBound, isAbstract)
		else
			LocalVariableDeclaration(source, targetScope, name, staticType)
		addUnits(valueDeclaration)
		valueDeclaration.declare()
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Class {
		determineTypes()
		val superType = superType?.withTypeSubstitutions(typeSubstitutions)
		return Class(source, name, scope.withTypeSubstitutions(typeSubstitutions, superType?.interfaceScope), explicitParentType,
			superType, members, isAbstract, isBound, isNative, isMutable, true)
	}

	override fun validate() {
		super.validate()
		if(!isAbstract)
			scope.ensureNoAbstractMembers() //TODO what about abstract members in objects and enums?
	}
}
