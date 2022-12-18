package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.semantic_model.control_flow.LoopStatement
import components.semantic_analysis.semantic_model.definitions.IndexOperatorDefinition
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.OperatorDefinition
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import java.util.*

abstract class Scope {
	private val subscribedTypes = LinkedList<Type>()

	open fun subscribe(type: Type) {
		subscribedTypes.add(type)
	}

	protected fun onNewType(type: TypeDefinition) {
		for(subscriber in subscribedTypes)
			subscriber.onNewType(type)
	}

	protected fun onNewValue(value: InterfaceMember) {
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

	open fun getSurroundingDefinition(): TypeDefinition? = null

	open fun getSurroundingLoop(): LoopStatement? = null

	abstract fun resolveType(name: String): TypeDefinition?

	open fun resolveValue(variable: VariableValue): ValueDeclaration? = resolveValue(variable.name)

	abstract fun resolveValue(name: String): ValueDeclaration?

	fun resolveOperator(kind: OperatorDefinition.Kind): OperatorDefinition?
		= resolveOperator(kind, listOf())

	fun resolveOperator(kind: OperatorDefinition.Kind, suppliedType: Value): OperatorDefinition?
		= resolveOperator(kind, listOf(suppliedType))

	open fun resolveOperator(kind: OperatorDefinition.Kind, suppliedValues: List<Value>): OperatorDefinition? = null

	fun resolveIndexOperator(suppliedTypes: List<Type>, suppliedIndexValues: List<Value>,
							 suppliedParameterValue: Value?): IndexOperatorDefinition?
		= resolveIndexOperator(suppliedTypes, suppliedIndexValues, listOfNotNull(suppliedParameterValue))

	open fun resolveIndexOperator(suppliedTypes: List<Type>, suppliedIndexValues: List<Value>,
								  suppliedParameterValues: List<Value>): IndexOperatorDefinition? = null
}
