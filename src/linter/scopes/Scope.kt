package linter.scopes

import linter.elements.definitions.FunctionDefinition
import linter.elements.definitions.OperatorDefinition
import linter.elements.values.TypeDefinition
import linter.elements.values.Value
import linter.elements.values.VariableValueDeclaration

abstract class Scope {

	abstract fun resolveType(name: String): TypeDefinition?

	abstract fun resolveReference(name: String): VariableValueDeclaration?

	abstract fun resolveFunction(name: String, suppliedValues: List<Value>): FunctionDefinition?

	abstract fun resolveOperator(name: String, suppliedValues: List<Value>): OperatorDefinition?

	fun resolveOperator(name: String, suppliedValue: Value): OperatorDefinition?
		= resolveOperator(name, listOf(suppliedValue))
}