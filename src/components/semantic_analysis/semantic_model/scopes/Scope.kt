package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.semantic_model.control_flow.LoopStatement
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.definitions.FunctionSignature
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.types.FunctionType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.*
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

	open fun getSurroundingDefinition(): TypeDefinition? = null

	open fun getSurroundingFunction(): FunctionImplementation? = null

	open fun getSurroundingLoop(): LoopStatement? = null

	abstract fun resolveType(name: String): TypeDefinition?

	open fun resolveValue(variable: VariableValue): ValueDeclaration? = resolveValue(variable.name)

	abstract fun resolveValue(name: String): ValueDeclaration?

	fun resolveOperator(kind: Operator.Kind): FunctionSignature? = resolveOperator(kind, listOf())

	fun resolveOperator(kind: Operator.Kind, suppliedType: Value): FunctionSignature? = resolveOperator(kind, listOf(suppliedType))

	open fun resolveOperator(kind: Operator.Kind, suppliedValues: List<Value>): FunctionSignature? {
		val operator = resolveValue(kind.stringRepresentation)?.getLinkedType() as? FunctionType
		return operator?.resolveSignature(suppliedValues)
	}

	fun resolveIndexOperator(suppliedTypes: List<Type>, suppliedIndexValues: List<Value>,
							 suppliedParameterValue: Value?): FunctionSignature?
		= resolveIndexOperator(suppliedTypes, suppliedIndexValues, listOfNotNull(suppliedParameterValue))

	open fun resolveIndexOperator(suppliedTypes: List<Type>, suppliedIndexValues: List<Value>,
								  suppliedParameterValues: List<Value>): FunctionSignature? {
		val kind = if(suppliedParameterValues.isEmpty())
			Operator.Kind.BRACKETS_GET
		else
			Operator.Kind.BRACKETS_SET
		return resolveOperator(kind, listOf(*suppliedIndexValues.toTypedArray(), *suppliedParameterValues.toTypedArray()))
	}
}
