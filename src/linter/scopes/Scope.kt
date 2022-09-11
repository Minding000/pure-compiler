package linter.scopes

import linter.elements.definitions.IndexOperatorDefinition
import linter.elements.definitions.InitializerDefinition
import linter.elements.definitions.OperatorDefinition
import linter.elements.literals.Type
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration

abstract class Scope {
	private val subscribedTypes = mutableListOf<Type>()

	open fun subscribe(type: Type) {
		subscribedTypes.add(type)
	}

	protected fun onNewType(type: TypeDefinition) {
		for(subscriber in subscribedTypes)
			subscriber.onNewType(type)
	}

	protected fun onNewValue(value: VariableValueDeclaration) {
		for(subscriber in subscribedTypes)
			subscriber.onNewValue(value)
	}

	protected fun onNewInitializer(initializer: InitializerDefinition) {
		for(subscriber in subscribedTypes)
			subscriber.onNewInitializer(initializer)
	}

	protected fun onNewOperator(operator: OperatorDefinition) {
		for(subscriber in subscribedTypes)
			subscriber.onNewOperator(operator)
	}

	open fun resolveGenerics(type: Type?): Type? = type

	abstract fun resolveType(name: String): TypeDefinition?

	abstract fun resolveValue(name: String): VariableValueDeclaration?

	abstract fun resolveOperator(name: String, suppliedTypes: List<Type?>): OperatorDefinition?

	fun resolveOperator(name: String, suppliedType: Type?): OperatorDefinition?
		= resolveOperator(name, listOf(suppliedType))

	abstract fun resolveIndexOperator(suppliedIndexTypes: List<Type?>, suppliedParameterTypes: List<Type?>):
			IndexOperatorDefinition?
}