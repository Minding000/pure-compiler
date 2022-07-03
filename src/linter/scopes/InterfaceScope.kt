package linter.scopes

import linter.elements.definitions.FunctionDefinition
import linter.elements.definitions.IndexOperatorDefinition
import linter.elements.definitions.InitializerDefinition
import linter.elements.definitions.OperatorDefinition
import linter.elements.literals.Type
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration
import java.util.*
import kotlin.collections.HashMap

class InterfaceScope(): Scope() {
	val types = HashMap<String, TypeDefinition>()
	val values = HashMap<String, VariableValueDeclaration>()
	val initializers = LinkedList<InitializerDefinition>()
	val functions = HashMap<String, LinkedList<FunctionDefinition>>()
	val operators = LinkedList<OperatorDefinition>()
	val genericTypes = HashMap<Type, Type>()

	override fun resolveGenerics(type: Type?): Type? {
		return genericTypes[type] ?: type
	}

	override fun resolveValue(name: String): VariableValueDeclaration? {
		return values[name]
	}

	override fun resolveType(name: String): TypeDefinition? {
		return types[name]
	}

	fun resolveInitializer(suppliedTypes: List<Type?>, currentTypes: LinkedList<Type?> = LinkedList()): InitializerDefinition? {
		if(suppliedTypes.size == currentTypes.size)
			return resolveInitializerExact(currentTypes)
		val type = suppliedTypes[currentTypes.size]
		currentTypes.add(type)
		var initializer = resolveInitializer(suppliedTypes, currentTypes)
		if(initializer != null)
			return initializer
		currentTypes.pop()
		for((_, t) in genericTypes.filterValues { t -> t == type }) {
			currentTypes.add(t)
			initializer = resolveInitializer(suppliedTypes, currentTypes)
			if(initializer != null)
				return initializer
			currentTypes.pop()
		}
		return null
	}

	private fun resolveInitializerExact(suppliedTypes: List<Type?>): InitializerDefinition? {
		initializerIteration@for(initializer in initializers) {
			if(initializer.parameters.size != suppliedTypes.size)
				continue
			for(i in suppliedTypes.indices) {
				if(suppliedTypes[i]?.let { initializer.parameters[i].type?.accepts(it) } != true)
					continue@initializerIteration
			}
			return initializer
		}
		return null
	}

	override fun resolveFunction(name: String, suppliedTypes: List<Type?>): FunctionDefinition? {
		return resolveFunction(name, suppliedTypes, LinkedList())
	}

	private fun resolveFunction(name: String, suppliedTypes: List<Type?>, currentTypes: LinkedList<Type?>): FunctionDefinition? {
		if(suppliedTypes.size == currentTypes.size)
			return resolveFunctionExact(name, currentTypes)
		val type = suppliedTypes[currentTypes.size]
		currentTypes.add(type)
		var function = resolveFunction(name, suppliedTypes, currentTypes)
		if(function != null)
			return function
		currentTypes.pop()
		for((_, t) in genericTypes.filterValues { t -> t == type }) {
			currentTypes.add(t)
			function = resolveFunction(name, suppliedTypes, currentTypes)
			if(function != null)
				return function
			currentTypes.pop()
		}
		return null
	}

	private fun resolveFunctionExact(name: String, types: List<Type?>): FunctionDefinition? {
		functions[name]?.let { definitions ->
			functionIteration@for(function in definitions) {
				if(function.parameters.size != types.size)
					continue
				for(i in types.indices) {
					if(types[i]?.let { function.parameters[i].type?.accepts(it) } != true)
						continue@functionIteration
				}
				return function
			}
		}
		return null
	}

	override fun resolveOperator(name: String, suppliedTypes: List<Type?>):
			OperatorDefinition? {
		return resolveOperator(name, suppliedTypes, LinkedList())
	}

	private fun resolveOperator(name: String, suppliedTypes: List<Type?>, currentTypes: LinkedList<Type?>):
			OperatorDefinition? {
		if(suppliedTypes.size == currentTypes.size)
			return resolveOperatorExact(name, currentTypes)
		val type = suppliedTypes[currentTypes.size]
		currentTypes.add(type)
		var operator = resolveOperator(name, suppliedTypes, currentTypes)
		if(operator != null)
			return operator
		currentTypes.pop()
		for((_, t) in genericTypes.filterValues { t -> t == type }) {
			currentTypes.add(t)
			operator = resolveOperator(name, suppliedTypes, currentTypes)
			if(operator != null)
				return operator
			currentTypes.pop()
		}
		return null
	}

	private fun resolveOperatorExact(name: String, suppliedTypes: List<Type?>):
			OperatorDefinition? {
		operatorIteration@for(operator in operators) {
			if(operator.name != name)
				continue
			if(operator.parameters.size != suppliedTypes.size)
				continue
			for(i in suppliedTypes.indices) {
				if(suppliedTypes[i]?.let { operator.parameters[i].type?.accepts(it) } != true)
					continue@operatorIteration
			}
			return operator
		}
		return null
	}

	override fun resolveIndexOperator(name: String, suppliedIndexTypes: List<Type?>, suppliedParameterTypes: List<Type?>):
			IndexOperatorDefinition? {
		return resolveIndexOperator(name, suppliedIndexTypes, suppliedParameterTypes, LinkedList())
	}

	private fun resolveIndexOperator(name: String, suppliedIndexTypes: List<Type?>, suppliedParameterTypes: List<Type?>,
									 currentTypes: LinkedList<Type?>):
			IndexOperatorDefinition? {
		if(suppliedIndexTypes.size + suppliedParameterTypes.size == currentTypes.size)
			return resolveIndexOperatorExact(name, currentTypes.subList(0, suppliedIndexTypes.size),
				currentTypes.subList(suppliedIndexTypes.size, currentTypes.size))
		val type = if(currentTypes.size < suppliedIndexTypes.size)
			suppliedIndexTypes[currentTypes.size]
		else
			suppliedParameterTypes[currentTypes.size - suppliedIndexTypes.size]
		currentTypes.add(type)
		var indexOperator = resolveIndexOperator(name, suppliedIndexTypes, currentTypes)
		if(indexOperator != null)
			return indexOperator
		currentTypes.pop()
		for((_, t) in genericTypes.filterValues { t -> t == type }) {
			currentTypes.add(t)
			indexOperator = resolveIndexOperator(name, suppliedIndexTypes, currentTypes)
			if(indexOperator != null)
				return indexOperator
			currentTypes.pop()
		}
		return null
	}

	private fun resolveIndexOperatorExact(name: String, suppliedIndexTypes: List<Type?>, suppliedParameterTypes: List<Type?>):
			IndexOperatorDefinition? {
		indexOperatorIteration@for(operator in operators) {
			if(operator.name != name)
				continue
			if(operator !is IndexOperatorDefinition)
				continue
			if(operator.indices.size != suppliedIndexTypes.size)
				continue
			for(i in suppliedIndexTypes.indices) {
				if(suppliedIndexTypes[i]?.let { operator.indices[i].type?.accepts(it) } != true)
					continue@indexOperatorIteration
			}
			if(operator.parameters.size != suppliedParameterTypes.size)
				continue
			for(i in suppliedParameterTypes.indices) {
				if(suppliedParameterTypes[i]?.let { operator.parameters[i].type?.accepts(it) } != true)
					continue@indexOperatorIteration
			}
			return operator
		}
		return null
	}
}