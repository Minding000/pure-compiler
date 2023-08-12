package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.semantic_model.control_flow.LoopStatement
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.definitions.TypeDeclaration
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.semantic_analysis.semantic_model.values.VariableValue
import logger.issues.definition.Redeclaration

class BlockScope(private val parentScope: MutableScope): MutableScope() {
	var semanticModel: SemanticModel? = null
	private val typeDeclarations = HashMap<String, TypeDeclaration>()
	private val valueDeclarations = HashMap<String, ValueDeclaration>()

	override fun addTypeDeclaration(newTypeDeclaration: TypeDeclaration) {
		val existingValueDeclaration = parentScope.getTypeDeclaration(newTypeDeclaration.name)
			?: typeDeclarations.putIfAbsent(newTypeDeclaration.name, newTypeDeclaration)
		if(existingValueDeclaration != null) {
			newTypeDeclaration.context.addIssue(Redeclaration(newTypeDeclaration.source, "type", newTypeDeclaration.name,
				existingValueDeclaration.source))
			return
		}
	}

	override fun getTypeDeclaration(name: String): TypeDeclaration? {
		return typeDeclarations[name] ?: parentScope.getTypeDeclaration(name)
	}

	override fun addValueDeclaration(newValueDeclaration: ValueDeclaration) {
		val existingValueDeclaration = parentScope.getValueDeclaration(newValueDeclaration.name)
			?: valueDeclarations.putIfAbsent(newValueDeclaration.name, newValueDeclaration)
		if(existingValueDeclaration != null) {
			newValueDeclaration.context.addIssue(Redeclaration(newValueDeclaration.source, "value", newValueDeclaration.name,
				existingValueDeclaration.source))
			return
		}
	}

	override fun getValueDeclaration(name: String): ValueDeclaration? {
		return valueDeclarations[name] ?: parentScope.getValueDeclaration(name)
	}

	override fun getValueDeclaration(variable: VariableValue): ValueDeclaration? {
		val valueDeclaration = valueDeclarations[variable.name]
		if(valueDeclaration != null) {
			if(valueDeclaration.isBefore(variable))
				return valueDeclaration
		}
		return parentScope.getValueDeclaration(variable)
	}

	override fun getSurroundingTypeDeclaration(): TypeDeclaration? {
		return parentScope.getSurroundingTypeDeclaration()
	}

	override fun getSurroundingFunction(): FunctionImplementation? {
		return (semanticModel as? FunctionImplementation) ?: parentScope.getSurroundingFunction()
	}

	override fun getSurroundingLoop(): LoopStatement? {
		return (semanticModel as? LoopStatement) ?: parentScope.getSurroundingLoop()
	}
}
