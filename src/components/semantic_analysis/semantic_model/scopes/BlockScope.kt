package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.semantic_model.control_flow.LoopStatement
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.semantic_analysis.semantic_model.values.VariableValue
import logger.issues.definition.Redeclaration

class BlockScope(private val parentScope: MutableScope): MutableScope() {
	var semanticModel: SemanticModel? = null
	val types = HashMap<String, TypeDefinition>()
	val values = HashMap<String, ValueDeclaration>()

	override fun declareType(typeDefinition: TypeDefinition) {
		val previousDeclaration = parentScope.resolveType(typeDefinition.name) ?: types.putIfAbsent(typeDefinition.name, typeDefinition)
		if(previousDeclaration != null) {
			typeDefinition.context.addIssue(Redeclaration(typeDefinition.source, "type", typeDefinition.name,
				previousDeclaration.source))
			return
		}
		onNewType(typeDefinition)
	}

	override fun getSurroundingDefinition(): TypeDefinition? {
		return parentScope.getSurroundingDefinition()
	}

	override fun getSurroundingFunction(): FunctionImplementation? {
		return (semanticModel as? FunctionImplementation) ?: parentScope.getSurroundingFunction()
	}

	override fun getSurroundingLoop(): LoopStatement? {
		return (semanticModel as? LoopStatement) ?: parentScope.getSurroundingLoop()
	}

	override fun resolveType(name: String): TypeDefinition? {
		return types[name] ?: parentScope.resolveType(name)
	}

	override fun declareValue(valueDeclaration: ValueDeclaration) {
		val previousDeclaration = parentScope.resolveValue(valueDeclaration.name) ?: values.putIfAbsent(valueDeclaration.name, valueDeclaration)
		if(previousDeclaration != null) {
			valueDeclaration.context.addIssue(Redeclaration(valueDeclaration.source, "value", valueDeclaration.name,
				previousDeclaration.source))
			return
		}
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
