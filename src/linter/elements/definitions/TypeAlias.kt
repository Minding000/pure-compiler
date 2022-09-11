package linter.elements.definitions

import linter.elements.literals.ObjectType
import linter.elements.literals.Type
import linter.elements.values.TypeDefinition
import linter.scopes.TypeScope
import parsing.ast.definitions.TypeAlias as ASTTypeAlias

class TypeAlias(override val source: ASTTypeAlias, name: String, val referenceType: Type, scope: TypeScope):
	TypeDefinition(source, name, scope, null) {
	private val specificDefinitions = HashMap<Map<ObjectType, Type>, TypeAlias>()

	override fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): TypeAlias {
		var definition = specificDefinitions[typeSubstitution]
		if(definition == null) {
			definition = TypeAlias(source, name, referenceType.withTypeSubstitutions(typeSubstitution),
				scope.withTypeSubstitutions(typeSubstitution, null))
			specificDefinitions[typeSubstitution] = definition
		}
		return definition
	}
}