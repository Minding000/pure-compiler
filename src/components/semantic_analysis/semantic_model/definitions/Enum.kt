package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.VariableValueDeclaration
import components.syntax_parser.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Enum(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, superType: Type?):
	TypeDefinition(source, name, scope, superType) {
	private val specificDefinitions = HashMap<Map<TypeDefinition, Type>, Enum>()

	init {
		scope.typeDefinition = this
	}

	override fun register(linter: Linter, parentScope: MutableScope) {
		parentScope.declareType(linter, this)
		val valueDeclaration = VariableValueDeclaration(source, name, StaticType(this))
		parentScope.declareValue(linter, valueDeclaration)
		units.add(valueDeclaration)
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<TypeDefinition, Type>): Enum {
		var definition = specificDefinitions[typeSubstitution]
		if(definition == null) {
			val superType = superType?.withTypeSubstitutions(typeSubstitution)
			definition = Enum(source, name, scope.withTypeSubstitutions(typeSubstitution, superType?.scope), superType)
			specificDefinitions[typeSubstitution] = definition
		}
		return definition
	}
}
