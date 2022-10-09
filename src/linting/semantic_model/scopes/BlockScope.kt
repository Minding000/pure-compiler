package linting.semantic_model.scopes

import linting.Linter
import linting.semantic_model.definitions.TypeDefinition
import linting.semantic_model.literals.Type
import linting.semantic_model.values.VariableValueDeclaration
import messages.Message

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
			linter.addMessage(type.source, "Declaration of type '${type.name}'.", Message.Type.DEBUG)
		} else {
			linter.addMessage(type.source, "Redeclaration of type '${type.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR)
		}
	}

	override fun resolveType(name: String): TypeDefinition? {
		return types[name] ?: parentScope.resolveType(name)
	}

	override fun declareValue(linter: Linter, value: VariableValueDeclaration) {
		val previousDeclaration = parentScope.resolveValue(value.name) ?: values.putIfAbsent(value.name, value)
		if(previousDeclaration == null) {
			onNewValue(value)
			linter.addMessage(value.source, "Declaration of value '${value.name}'.", Message.Type.DEBUG)
		} else {
			linter.addMessage(value.source, "Redeclaration of value '${value.name}'," +
					" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR)
		}
	}

	override fun resolveValue(name: String): VariableValueDeclaration? {
		return values[name] ?: parentScope.resolveValue(name)
	}
}
