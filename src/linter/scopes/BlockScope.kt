package linter.scopes

import linter.Linter
import linter.elements.definitions.IndexOperatorDefinition
import linter.elements.definitions.OperatorDefinition
import linter.elements.literals.Type
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration
import linter.messages.Message
import kotlin.collections.HashMap

class BlockScope(val parentScope: MutableScope): MutableScope() {
	val types = HashMap<String, TypeDefinition>()
	val values = HashMap<String, VariableValueDeclaration>()

	override fun subscribe(type: Type) {
		super.subscribe(type)
		for((_, typeDefinition) in types)
			type.onNewType(typeDefinition)
		for((_, value) in values)
			type.onNewValue(value)
	}

	override fun declareType(linter: Linter, type: TypeDefinition) {
		val previousDeclaration = parentScope.resolveType(type.name) ?: types.putIfAbsent(type.name, type)
		if(previousDeclaration == null) {
			onNewType(type)
			linter.messages.add(Message(
				"${type.source.getStartString()}: Declaration of type '${type.name}'.", Message.Type.DEBUG))
		} else {
			linter.messages.add(Message(
				"${type.source.getStartString()}: Redeclaration of type '${type.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
		}
	}

	override fun resolveType(name: String): TypeDefinition? {
		return types[name] ?: parentScope.resolveType(name)
	}

	override fun declareValue(linter: Linter, value: VariableValueDeclaration) {
		val previousDeclaration = parentScope.resolveValue(value.name) ?: values.putIfAbsent(value.name, value)
		if(previousDeclaration == null) {
			onNewValue(value)
			linter.messages.add(Message(
					"${value.source.getStartString()}: Declaration of value '${value.name}'.", Message.Type.DEBUG))
		} else {
			linter.messages.add(Message(
				"${value.source.getStartString()}: Redeclaration of value '${value.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
		}
	}

	override fun resolveValue(name: String): VariableValueDeclaration? {
		return values[name] ?: parentScope.resolveValue(name)
	}

	override fun resolveOperator(name: String, suppliedTypes: List<Type?>): OperatorDefinition? {
		return null
	}

	override fun resolveIndexOperator(name: String, suppliedIndexTypes: List<Type?>, suppliedParameterTypes: List<Type?>):
			IndexOperatorDefinition? {
		return null
	}
}