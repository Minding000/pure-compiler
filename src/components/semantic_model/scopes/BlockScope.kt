package components.semantic_model.scopes

import components.semantic_model.control_flow.LoopStatement
import components.semantic_model.declarations.*
import components.semantic_model.general.ErrorHandlingContext
import components.semantic_model.general.HandleBlock
import components.semantic_model.general.SemanticModel
import components.semantic_model.values.VariableValue
import logger.issues.declaration.Redeclaration

class BlockScope(val parentScope: MutableScope): MutableScope() {
	var semanticModel: SemanticModel? = null
	private val typeDeclarations = HashMap<String, TypeDeclaration>()
	private val valueDeclarations = HashMap<String, ValueDeclaration>()

	override fun addTypeDeclaration(newTypeDeclaration: TypeDeclaration) {
		val existingValueDeclaration = typeDeclarations.putIfAbsent(newTypeDeclaration.name, newTypeDeclaration)
		if(existingValueDeclaration != null)
			newTypeDeclaration.context.addIssue(Redeclaration(newTypeDeclaration.source, "type", newTypeDeclaration.name,
				existingValueDeclaration.source))
	}

	override fun getTypeDeclaration(name: String): TypeDeclaration? {
		return typeDeclarations[name] ?: parentScope.getTypeDeclaration(name)
	}

	override fun addValueDeclaration(newValueDeclaration: ValueDeclaration) {
		val existingValueDeclaration = valueDeclarations.putIfAbsent(newValueDeclaration.name, newValueDeclaration)
		if(existingValueDeclaration != null)
			newValueDeclaration.context.addIssue(Redeclaration(newValueDeclaration.source, "value", newValueDeclaration.name,
				existingValueDeclaration.source))
	}

	override fun getValueDeclaration(name: String): ValueDeclaration.Match? {
		val valueDeclaration = valueDeclarations[name] ?: return parentScope.getValueDeclaration(name)
		return ValueDeclaration.Match(valueDeclaration)
	}

	override fun getValueDeclaration(variable: VariableValue): ValueDeclaration.Match? {
		val valueDeclaration = valueDeclarations[variable.name]
		if(valueDeclaration != null) {
			if(valueDeclaration.isBefore(variable))
				return ValueDeclaration.Match(valueDeclaration)
		}
		return parentScope.getValueDeclaration(variable)
	}

	override fun getSurroundingTypeDeclaration(): TypeDeclaration? {
		return parentScope.getSurroundingTypeDeclaration()
	}

	override fun getSurroundingInitializer(): InitializerDefinition? {
		return (semanticModel as? InitializerDefinition) ?: parentScope.getSurroundingInitializer()
	}

	override fun getSurroundingFunction(): FunctionImplementation? {
		return (semanticModel as? FunctionImplementation) ?: parentScope.getSurroundingFunction()
	}

	override fun getSurroundingComputedProperty(): ComputedPropertyDeclaration? {
		return (semanticModel as? ComputedPropertyDeclaration) ?: parentScope.getSurroundingComputedProperty()
	}

	override fun getSurroundingLoop(): LoopStatement? {
		return (semanticModel as? LoopStatement) ?: parentScope.getSurroundingLoop()
	}

	override fun getSurroundingErrorHandlingContext(): Pair<ErrorHandlingContext, SemanticModel>? {
		var semanticModel = semanticModel
		if(semanticModel?.parent is HandleBlock)
			semanticModel = semanticModel.parent
		val errorHandlingContext = semanticModel?.parent as? ErrorHandlingContext ?: return parentScope.getSurroundingErrorHandlingContext()
		return Pair(errorHandlingContext, semanticModel)
	}

	override fun getSurroundingAlwaysBlock(): ErrorHandlingContext? {
		val errorHandlingContext = semanticModel?.parent as? ErrorHandlingContext ?: return parentScope.getSurroundingAlwaysBlock()
		if(errorHandlingContext.alwaysBlock == null || errorHandlingContext.alwaysBlock == semanticModel)
			return parentScope.getSurroundingAlwaysBlock()
		return errorHandlingContext
	}

	fun validate() {
		validateDeclarations()
	}

	private fun validateDeclarations() {
		for((name, valueDeclaration) in valueDeclarations) {
			val parentValueDeclaration = parentScope.getValueDeclaration(name)?.declaration ?: continue
			if(parentValueDeclaration.scope is BlockScope && parentValueDeclaration.isBefore(valueDeclaration)) {
				valueDeclaration.context.addIssue(Redeclaration(valueDeclaration.source, "value", valueDeclaration.name,
					parentValueDeclaration.source))
			}
		}
	}
}
