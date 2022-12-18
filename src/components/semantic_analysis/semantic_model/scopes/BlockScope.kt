package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.control_flow.LoopStatement
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.semantic_analysis.semantic_model.values.VariableValue
import messages.Message

class BlockScope(val parentScope: MutableScope): MutableScope() {
	var unit: Unit? = null
	val types = HashMap<String, TypeDefinition>()
	val values = HashMap<String, ValueDeclaration>()

	override fun declareType(linter: Linter, type: TypeDefinition) {
		val previousDeclaration = parentScope.resolveType(type.name) ?: types.putIfAbsent(type.name, type)
		if(previousDeclaration != null) {
			linter.addMessage(type.source, "Redeclaration of type '${type.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR)
			return
		}
		onNewType(type)
		linter.addMessage(type.source, "Declaration of type '${type.name}'.", Message.Type.DEBUG)
	}

	override fun getSurroundingDefinition(): TypeDefinition? {
		return parentScope.getSurroundingDefinition()
	}

	override fun getSurroundingLoop(): LoopStatement? {
		return (unit as? LoopStatement) ?: parentScope.getSurroundingLoop()
	}

	override fun resolveType(name: String): TypeDefinition? {
		return types[name] ?: parentScope.resolveType(name)
	}

	override fun declareValue(linter: Linter, value: ValueDeclaration) {
		val previousDeclaration = parentScope.resolveValue(value.name) ?: values.putIfAbsent(value.name, value)
		if(previousDeclaration != null) {
			linter.addMessage(value.source, "Redeclaration of value '${value.name}'," +
					" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR)
			return
		}
		linter.addMessage(value.source, "Declaration of value '${value.name}'.", Message.Type.DEBUG)
	}

	override fun resolveValue(name: String): ValueDeclaration? {
		return values[name] ?: parentScope.resolveValue(name)
	}

	override fun resolveValue(variable: VariableValue): ValueDeclaration? {
		val definition = values[variable.name]
		if(definition != null) {
			if(definition.isBefore(variable))
				return definition
		}
		return parentScope.resolveValue(variable)
	}
}
