package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import logger.issues.definition.Redeclaration

class FileScope: MutableScope() {
	private val referencedTypes = HashMap<String, TypeDefinition>()
	val types = HashMap<String, TypeDefinition>()
	private val referencedValues = HashMap<String, ValueDeclaration>()
	val values = HashMap<String, ValueDeclaration>()

	fun reference(scope: FileScope) {
		referencedTypes.putAll(scope.types)
		referencedValues.putAll(scope.values)
	}

	override fun declareType(linter: Linter, typeDefinition: TypeDefinition) {
		val previousDeclaration = referencedTypes[typeDefinition.name] ?: types.putIfAbsent(typeDefinition.name, typeDefinition)
		if(previousDeclaration != null) {
			linter.addIssue(Redeclaration(typeDefinition.source, "type", typeDefinition.name, previousDeclaration.source))
			return
		}
		onNewType(typeDefinition)
	}

	override fun resolveType(name: String): TypeDefinition? {
		return types[name] ?: referencedTypes[name]
	}

	override fun declareValue(linter: Linter, valueDeclaration: ValueDeclaration) {
		val previousDeclaration = referencedValues[valueDeclaration.name] ?: values.putIfAbsent(valueDeclaration.name, valueDeclaration)
		if(previousDeclaration != null) {
			linter.addIssue(Redeclaration(valueDeclaration.source, "value", valueDeclaration.name, previousDeclaration.source))
			return
		}
	}

	override fun resolveValue(name: String): ValueDeclaration? {
		return values[name] ?: referencedValues[name]
	}
}
