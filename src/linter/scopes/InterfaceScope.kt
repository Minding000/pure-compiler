package linter.scopes

import linter.Linter
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration
import linter.messages.Message
import kotlin.collections.HashMap

class InterfaceScope: Scope() {
	val declaredTypes = HashMap<String, TypeDefinition>()
	val declaredValues = HashMap<String, VariableValueDeclaration>()

	override fun declareType(linter: Linter, type: TypeDefinition) {
		val previousDeclaration = declaredTypes.putIfAbsent(type.name, type)
		if(previousDeclaration != null)
			linter.messages.add(Message(
				"${type.source.getStartString()}: Redeclaration of type '${type.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
	}

	override fun resolveType(name: String): TypeDefinition? {
		return declaredTypes[name]
	}

	override fun declareValue(linter: Linter, value: VariableValueDeclaration) {
		val previousDeclaration = declaredValues.putIfAbsent(value.name, value)
		if(previousDeclaration != null)
			linter.messages.add(Message(
				"${value.source.getStartString()}: Redeclaration of value '${value.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
	}

	override fun resolveReference(name: String): VariableValueDeclaration? {
		return declaredValues[name]
	}
}