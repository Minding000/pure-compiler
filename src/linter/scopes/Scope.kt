package linter.scopes

import linter.elements.definitions.FunctionDefinition
import linter.elements.definitions.IndexOperatorDefinition
import linter.elements.definitions.OperatorDefinition
import linter.elements.literals.Type
import linter.elements.values.TypeDefinition
import linter.elements.values.Value
import linter.elements.values.VariableValueDeclaration

abstract class Scope {

	open fun resolveGenerics(type: Type?): Type? = type

	abstract fun resolveType(name: String): TypeDefinition?

	abstract fun resolveValue(name: String): VariableValueDeclaration?

	abstract fun resolveFunction(name: String, suppliedTypes: List<Type?>): FunctionDefinition?

	abstract fun resolveOperator(name: String, suppliedTypes: List<Type?>): OperatorDefinition?

	fun resolveOperator(name: String, suppliedType: Type?): OperatorDefinition?
		= resolveOperator(name, listOf(suppliedType))

	abstract fun resolveIndexOperator(name: String, suppliedIndexTypes: List<Type?>, suppliedParameterTypes: List<Type?>):
			IndexOperatorDefinition?
}