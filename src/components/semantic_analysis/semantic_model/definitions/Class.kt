package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.LocalVariableDeclaration
import components.tokenizer.WordAtom
import components.syntax_parser.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Class(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, explicitParentType: ObjectType?,
			superType: Type?, val isAbstract: Boolean, isBound: Boolean, val isNative: Boolean, val isMutable: Boolean):
	TypeDefinition(source, name, scope, explicitParentType, superType, isBound) {

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.ABSTRACT, WordAtom.BOUND, WordAtom.IMMUTABLE, WordAtom.NATIVE)
	}

	init {
		scope.typeDefinition = this
	}

	override fun register(linter: Linter, parentScope: MutableScope) {
		val targetScope = parentTypeDefinition?.scope ?: parentScope
		targetScope.declareType(linter, this)
		val staticType = StaticType(this)
		addUnits(staticType)
		val valueDeclaration = if(targetScope is TypeScope)
			PropertyDeclaration(source, targetScope, name, staticType, null, !isBound, isAbstract)
		else
			LocalVariableDeclaration(source, targetScope, name, staticType)
		targetScope.declareValue(linter, valueDeclaration)
		addUnits(valueDeclaration)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Class {
		val superType = superType?.withTypeSubstitutions(typeSubstitutions)
		return Class(source, name, scope.withTypeSubstitutions(typeSubstitutions, superType?.interfaceScope), explicitParentType, superType,
			isAbstract, isBound, isNative, isMutable)
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		if(!isAbstract)
			scope.ensureNoAbstractMembers(linter) //TODO what about abstract members in objects and enums?
	}
}
