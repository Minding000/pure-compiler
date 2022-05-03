package linter.scopes

import linter.Linter
import linter.elements.definitions.FunctionDefinition
import linter.elements.definitions.OperatorDefinition
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration
import linter.messages.Message
import kotlin.collections.HashMap

class TypeScope(val parentScope: Scope, val superScope: InterfaceScope?): Scope() {
	val declaredTypes = HashMap<String, TypeDefinition>()
	val declaredValues = HashMap<String, VariableValueDeclaration>()
	val functions = HashMap<String, HashMap<String, FunctionDefinition>>()
	val operators = HashMap<String, HashMap<String, OperatorDefinition>>()

	override fun declareType(linter: Linter, type: TypeDefinition) {
		var previousDeclaration = parentScope.resolveType(type.name)
		if(previousDeclaration != null) {
			linter.messages.add(Message(
				"${type.source.getStartString()}: '${type.name}' shadows a variable.", Message.Type.WARNING))
			return
		}
		previousDeclaration = superScope?.resolveType(type.name)
		if(previousDeclaration != null) {
			linter.messages.add(Message(
				"${type.source.getStartString()}: Redeclaration of type '${type.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}." +
						" Use the 'override' keyword to modify it.", Message.Type.ERROR))
			return
		}
		previousDeclaration = declaredTypes.putIfAbsent(type.name, type)
		if(previousDeclaration != null)
			linter.messages.add(Message(
				"${type.source.getStartString()}: Redeclaration of type '${type.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
	}

	override fun resolveType(name: String): TypeDefinition? {
		return parentScope.resolveType(name)
			?: superScope?.resolveType(name)
			?: declaredTypes[name]
	}

	override fun declareValue(linter: Linter, value: VariableValueDeclaration) {
		var previousDeclaration = parentScope.resolveReference(value.name)
		if(previousDeclaration != null) {
			linter.messages.add(Message(
				"${value.source.getStartString()}: '${value.name}' shadows a variable.", Message.Type.WARNING))
			return
		}
		previousDeclaration = superScope?.resolveReference(value.name)
		if(previousDeclaration != null) {
			linter.messages.add(Message(
				"${value.source.getStartString()}: Redeclaration of type '${value.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}." +
						" Use the 'override' keyword to modify it.", Message.Type.ERROR))
			return
		}
		previousDeclaration = declaredValues.putIfAbsent(value.name, value)
		if(previousDeclaration != null)
			linter.messages.add(Message(
				"${value.source.getStartString()}: Redeclaration of value '${value.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
	}

	override fun resolveReference(name: String): VariableValueDeclaration? {
		return parentScope.resolveReference(name)
			?: superScope?.resolveReference(name)
			?: declaredValues[name]
	}

	override fun declareFunction(linter: Linter, function: FunctionDefinition) {
		val signatures = functions.getOrPut(function.name) { HashMap() }
		val previousDeclaration = signatures.putIfAbsent(function.variation, function)
		if(previousDeclaration != null)
			linter.messages.add(Message(
				"${function.source.getStartString()}: Redeclaration of function '${function.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
	}

	override fun declareOperator(linter: Linter, operator: OperatorDefinition) {
		val signatures = operators.getOrPut(operator.name) { HashMap() }
		val previousDeclaration = signatures.putIfAbsent(operator.variation, operator)
		if(previousDeclaration != null)
			linter.messages.add(Message(
				"${operator.source.getStartString()}: Redeclaration of operator '${operator.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
	}
}