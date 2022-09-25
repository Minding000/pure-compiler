package linting.semantic_model.scopes

import linting.semantic_model.definitions.IndexOperatorDefinition
import linting.semantic_model.definitions.InitializerDefinition
import linting.semantic_model.definitions.OperatorDefinition
import linting.semantic_model.literals.Type
import linting.semantic_model.definitions.TypeDefinition
import linting.semantic_model.values.VariableValueDeclaration

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

	abstract fun resolveType(name: String): TypeDefinition?

	abstract fun resolveValue(name: String): VariableValueDeclaration?

	fun resolveOperator(name: String): OperatorDefinition?
		= resolveOperator(name, listOf())

	fun resolveOperator(name: String, suppliedType: Type?): OperatorDefinition?
		= resolveOperator(name, listOf(suppliedType))

	abstract fun resolveOperator(name: String, suppliedTypes: List<Type?>): OperatorDefinition?

	abstract fun resolveIndexOperator(suppliedIndexTypes: List<Type?>, suppliedParameterTypes: List<Type?>):
			IndexOperatorDefinition?
}