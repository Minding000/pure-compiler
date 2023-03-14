package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.control_flow.LoopStatement
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.semantic_analysis.semantic_model.values.VariableValue
import logger.issues.definition.Declaration
import logger.issues.definition.Redeclaration

class BlockScope(val parentScope: MutableScope): MutableScope() {
	var unit: Unit? = null
	val types = HashMap<String, TypeDefinition>()
	val values = HashMap<String, ValueDeclaration>()

	override fun declareType(linter: Linter, type: TypeDefinition) {
		val previousDeclaration = parentScope.resolveType(type.name) ?: types.putIfAbsent(type.name, type)
		if(previousDeclaration != null) {
			linter.addIssue(Redeclaration(type.source, "type", type.name, previousDeclaration.source))
			return
		}
		onNewType(type)
		linter.addIssue(Declaration(type.source, "type", type.name))
	}

	override fun getSurroundingDefinition(): TypeDefinition? {
		return parentScope.getSurroundingDefinition()
	}

	override fun getSurroundingFunction(): FunctionImplementation? {
		return (unit as? FunctionImplementation) ?: parentScope.getSurroundingFunction()
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
			linter.addIssue(Redeclaration(value.source, "value", value.name, previousDeclaration.source))
			return
		}
		linter.addIssue(Declaration(value.source, "value", value.name))
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
