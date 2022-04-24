package linter.scopes

import linter.Linter
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration

abstract class Scope {

	abstract fun declareType(linter: Linter, type: TypeDefinition)

	abstract fun resolveType(name: String): TypeDefinition?

	abstract fun declareValue(linter: Linter, value: VariableValueDeclaration)

	abstract fun resolveReference(name: String): VariableValueDeclaration?
}