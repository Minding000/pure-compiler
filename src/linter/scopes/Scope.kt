package linter.scopes

import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration

abstract class Scope {

	abstract fun declareType(type: TypeDefinition)

	abstract fun resolveType(name: String): TypeDefinition?

	abstract fun declareValue(value: VariableValueDeclaration)

	abstract fun resolveReference(name: String): VariableValueDeclaration?
}