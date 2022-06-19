package linter.scopes

import linter.Linter
import linter.elements.definitions.FunctionDefinition
import linter.elements.definitions.OperatorDefinition
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration
import linter.messages.Message
import kotlin.collections.HashMap

class FileScope: MutableScope() {
	private val referencedTypes = HashMap<String, TypeDefinition>()
	val declaredTypes = HashMap<String, TypeDefinition>()
	val declaredValues = HashMap<String, VariableValueDeclaration>()

	fun referenceTypes(types: HashMap<String, TypeDefinition>) {
		declaredTypes.putAll(types)
	}

	override fun declareType(linter: Linter, type: TypeDefinition) {
		val previousDeclaration = referencedTypes[type.name] ?: declaredTypes.putIfAbsent(type.name, type)
		if(previousDeclaration == null)
			linter.messages.add(Message(
				"${type.source.getStartString()}: Declaration of type '${type.name}'.", Message.Type.DEBUG))
		else
			linter.messages.add(Message(
				"${type.source.getStartString()}: Redeclaration of type '${type.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
	}

	override fun resolveType(name: String): TypeDefinition? {
		return declaredTypes[name] ?: referencedTypes[name]
	}

	override fun declareValue(linter: Linter, value: VariableValueDeclaration) {
		val previousDeclaration = declaredValues.putIfAbsent(value.name, value)
		if(previousDeclaration == null)
			linter.messages.add(Message(
				"${value.source.getStartString()}: Declaration of value '${value.name}'.", Message.Type.DEBUG))
		else
			linter.messages.add(Message(
				"${value.source.getStartString()}: Redeclaration of value '${value.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
	}

	override fun resolveReference(name: String): VariableValueDeclaration? {
		return declaredValues[name]
	}

	override fun resolveFunction(name: String, variation: String): FunctionDefinition? {
		return null
	}

	override fun resolveOperator(name: String, variation: String): OperatorDefinition? {
		return null
	}
}