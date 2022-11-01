package components.linting.semantic_model.definitions

import components.linting.Linter
import components.linting.semantic_model.scopes.MutableScope
import components.linting.semantic_model.scopes.TypeScope
import components.linting.semantic_model.types.ObjectType
import components.linting.semantic_model.types.Type
import components.linting.semantic_model.values.VariableValueDeclaration
import components.tokenizer.WordAtom
import components.syntax_parser.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Object(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, superType: Type?,
			 val isNative: Boolean, val isMutable: Boolean): TypeDefinition(source, name, scope, superType) {

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE, WordAtom.IMMUTABLE)
	}

	init {
		scope.typeDefinition = this
	}

	override fun register(linter: Linter, parentScope: MutableScope) {
		parentScope.declareType(linter, this)
		val valueDeclaration = VariableValueDeclaration(source, name, ObjectType(this))
		parentScope.declareValue(linter, valueDeclaration)
		units.add(valueDeclaration)
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<TypeDefinition, Type>): Object {
		return this
	}
}
