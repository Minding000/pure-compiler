package linter.elements.definitions

import linter.elements.literals.ObjectType
import linter.elements.literals.Type
import linter.elements.values.TypeDefinition
import linter.scopes.TypeScope
import parsing.ast.definitions.GenericsListElement

class GenericTypeDefinition(override val source: GenericsListElement, name: String, scope: TypeScope, superType: Type?):
	TypeDefinition(source, name, scope, superType) {
	private val specificDefinitions = HashMap<Map<ObjectType, Type>, GenericTypeDefinition>()

	override fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): GenericTypeDefinition {
		var definition = specificDefinitions[typeSubstitution]
		if(definition == null) {
			val superType = superType?.withTypeSubstitutions(typeSubstitution)
			definition = GenericTypeDefinition(source, name, scope.withTypeSubstitutions(typeSubstitution, superType?.scope), superType)
			specificDefinitions[typeSubstitution] = definition
		}
		return definition
	}
}