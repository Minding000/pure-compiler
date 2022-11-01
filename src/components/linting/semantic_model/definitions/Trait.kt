package components.linting.semantic_model.definitions

import components.linting.Linter
import components.linting.semantic_model.scopes.MutableScope
import components.linting.semantic_model.scopes.TypeScope
import components.linting.semantic_model.types.Type
import components.parsing.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Trait(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, superType: Type?):
	TypeDefinition(source, name, scope, superType) {
	private val specificDefinitions = HashMap<Map<TypeDefinition, Type>, Trait>()

	init {
		scope.typeDefinition = this
	}

	override fun register(linter: Linter, parentScope: MutableScope) {
		parentScope.declareType(linter, this)
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<TypeDefinition, Type>): Trait {
		var definition = specificDefinitions[typeSubstitution]
		if(definition == null) {
			val superType = superType?.withTypeSubstitutions(typeSubstitution)
			definition = Trait(source, name, scope.withTypeSubstitutions(typeSubstitution, superType?.scope), superType)
			specificDefinitions[typeSubstitution] = definition
		}
		return definition
	}
}
