package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import logger.issues.definition.Declaration
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

	override fun declareType(linter: Linter, type: TypeDefinition) {
		val previousDeclaration = referencedTypes[type.name] ?: types.putIfAbsent(type.name, type)
		if(previousDeclaration != null) {
			linter.addIssue(Redeclaration(type.source, "type", type.name, previousDeclaration.source))
			return
		}
		onNewType(type)
		linter.addIssue(Declaration(type.source, "type", type.name))
	}

	override fun resolveType(name: String): TypeDefinition? {
		return types[name] ?: referencedTypes[name]
	}

	override fun declareValue(linter: Linter, value: ValueDeclaration) {
		val previousDeclaration = referencedValues[value.name] ?: values.putIfAbsent(value.name, value)
		if(previousDeclaration != null) {
			linter.addIssue(Redeclaration(value.source, "value", value.name, previousDeclaration.source))
			return
		}
		linter.addIssue(Declaration(value.source, "value", value.name))
	}

	override fun resolveValue(name: String): ValueDeclaration? {
		return values[name] ?: referencedValues[name]
	}
}
