package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.LocalVariableDeclaration
import components.tokenizer.WordAtom
import components.syntax_parser.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Object(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, explicitParentType: ObjectType?,
			 superType: Type, isBound: Boolean, val isNative: Boolean, val isMutable: Boolean):
	TypeDefinition(source, name, scope, explicitParentType, superType, isBound) {

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.BOUND, WordAtom.NATIVE, WordAtom.IMMUTABLE)
	}

	init {
		scope.typeDefinition = this
	}

	override fun register(linter: Linter, parentScope: MutableScope) {
		val targetScope = parentTypeDefinition?.scope ?: parentScope
		targetScope.declareType(linter, this)
		val type = ObjectType(this)
		val valueDeclaration = if(targetScope is TypeScope)
			PropertyDeclaration(source, name, type, null, !isBound)
		else
			LocalVariableDeclaration(source, name, type)
		targetScope.declareValue(linter, valueDeclaration)
		addUnits(valueDeclaration)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Object {
		return this
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		scope.ensureTrivialInitializers(linter)
	}
}
