package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.VariableValueDeclaration
import components.tokenizer.WordAtom
import components.syntax_parser.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Class(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, superType: Type?,
			val isNative: Boolean, val isMutable: Boolean): TypeDefinition(source, name, scope, superType) {
	private var specificDefinitions = HashMap<Map<TypeDefinition, Type>, Class>()

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE, WordAtom.IMMUTABLE)
	}

	init {
		scope.typeDefinition = this
	}

	override fun register(linter: Linter, parentScope: MutableScope) {
		parentScope.declareType(linter, this)
		val valueDeclaration = VariableValueDeclaration(source, name, StaticType(this))
		parentScope.declareValue(linter, valueDeclaration)
		units.add(valueDeclaration)
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<TypeDefinition, Type>): Class {
		var definition = specificDefinitions[typeSubstitution]
		if(definition == null) {
			val superType = superType?.withTypeSubstitutions(typeSubstitution)
			definition = Class(source, name, scope.withTypeSubstitutions(typeSubstitution, superType?.scope), superType,
				isNative, isMutable)
			specificDefinitions[typeSubstitution] = definition
		}
		return definition
	}
}
