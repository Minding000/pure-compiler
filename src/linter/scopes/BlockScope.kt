package linter.scopes

import linter.Linter
import linter.elements.definitions.FunctionDefinition
import linter.elements.definitions.IndexOperatorDefinition
import linter.elements.definitions.OperatorDefinition
import linter.elements.literals.Type
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration
import linter.messages.Message
import kotlin.collections.HashMap

class BlockScope(val parentScope: MutableScope): MutableScope() {
	val declaredTypes = HashMap<String, TypeDefinition>()
	val declaredValues = HashMap<String, VariableValueDeclaration>()

	override fun declareType(linter: Linter, type: TypeDefinition) {
		val previousDeclaration = parentScope.resolveType(type.name) ?: declaredTypes.putIfAbsent(type.name, type)
		if(previousDeclaration == null)
			linter.messages.add(Message(
				"${type.source.getStartString()}: Declaration of type '${type.name}'.", Message.Type.DEBUG))
		else
			linter.messages.add(Message(
				"${type.source.getStartString()}: Redeclaration of type '${type.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
	}

	override fun resolveType(name: String): TypeDefinition? {
		return declaredTypes[name] ?: parentScope.resolveType(name)
	}

	override fun declareValue(linter: Linter, value: VariableValueDeclaration) {
		val previousDeclaration = parentScope.resolveValue(value.name) ?: declaredValues.putIfAbsent(value.name, value)
		if(previousDeclaration == null)
			linter.messages.add(Message(
				"${value.source.getStartString()}: Declaration of value '${value.name}'.", Message.Type.DEBUG))
		else
			linter.messages.add(Message(
				"${value.source.getStartString()}: Redeclaration of value '${value.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
	}

	override fun resolveValue(name: String): VariableValueDeclaration? {
		return declaredValues[name] ?: parentScope.resolveValue(name)
	}

	override fun resolveFunction(name: String, suppliedTypes: List<Type?>): FunctionDefinition? {
		return parentScope.resolveFunction(name, suppliedTypes)
	}

	override fun resolveOperator(name: String, suppliedTypes: List<Type?>): OperatorDefinition? {
		return null
	}

	override fun resolveIndexOperator(name: String, suppliedIndexTypes: List<Type?>, suppliedParameterTypes: List<Type?>):
			IndexOperatorDefinition? {
		return null
	}
}