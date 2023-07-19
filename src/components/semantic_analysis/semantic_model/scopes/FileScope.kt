package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import logger.issues.definition.Redeclaration

class FileScope: MutableScope() {
	private val referencedTypeDefinitions = HashMap<String, TypeDefinition>()
	val typeDefinitions = HashMap<String, TypeDefinition>()
	private val referencedValueDeclarations = HashMap<String, ValueDeclaration>()
	val valueDeclarations = HashMap<String, ValueDeclaration>()

	fun reference(scope: FileScope) {
		referencedTypeDefinitions.putAll(scope.typeDefinitions)
		referencedValueDeclarations.putAll(scope.valueDeclarations)
	}

	override fun declareType(typeDefinition: TypeDefinition) {
		val previousDeclaration = referencedTypeDefinitions[typeDefinition.name] ?: typeDefinitions.putIfAbsent(typeDefinition.name, typeDefinition)
		if(previousDeclaration != null) {
			typeDefinition.context.addIssue(Redeclaration(typeDefinition.source, "type", typeDefinition.name,
				previousDeclaration.source))
			return
		}
		onNewType(typeDefinition)
	}

	override fun resolveType(name: String): TypeDefinition? {
		return typeDefinitions[name] ?: referencedTypeDefinitions[name]
	}

	override fun declareValue(valueDeclaration: ValueDeclaration) {
		val previousDeclaration = referencedValueDeclarations[valueDeclaration.name] ?: valueDeclarations.putIfAbsent(valueDeclaration.name, valueDeclaration)
		if(previousDeclaration != null) {
			valueDeclaration.context.addIssue(Redeclaration(valueDeclaration.source, "value", valueDeclaration.name,
				previousDeclaration.source))
			return
		}
	}

	override fun resolveValue(name: String): ValueDeclaration? {
		return valueDeclarations[name] ?: referencedValueDeclarations[name]
	}
}
