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

class Enum(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, explicitParentType: ObjectType?, superType: Type?,
		   isBound: Boolean): TypeDefinition(source, name, scope, explicitParentType, superType, isBound) {

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.BOUND)
	}

	init {
		scope.typeDefinition = this
	}

	override fun register(linter: Linter, parentScope: MutableScope) {
		val targetScope = parentTypeDefinition?.scope ?: parentScope
		targetScope.declareType(linter, this)
		val staticType = StaticType(this)
		val valueDeclaration = if(targetScope is TypeScope)
			PropertyDeclaration(source, name, staticType, null, !isBound)
		else
			LocalVariableDeclaration(source, name, staticType)
		targetScope.declareValue(linter, valueDeclaration)
		addUnits(valueDeclaration)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Enum {
		val superType = superType?.withTypeSubstitutions(typeSubstitutions)
		return Enum(source, name, scope.withTypeSubstitutions(typeSubstitutions, superType?.scope), explicitParentType, superType, isBound)
	}
}
