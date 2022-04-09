package linter.scopes

import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration
import kotlin.collections.HashMap

class FileScope: Scope() {
	private val referencedTypes = HashMap<String, TypeDefinition>()
	val declaredTypes = HashMap<String, TypeDefinition>()
	val declaredValues = HashMap<String, VariableValueDeclaration>()

	fun referenceTypes(types: HashMap<String, TypeDefinition>) {
		declaredTypes.putAll(types)
	}

	override fun declareType(type: TypeDefinition) {
		declaredTypes[type.name] = type
	}

	override fun resolveType(name: String): TypeDefinition? {
		return declaredTypes[name] ?: referencedTypes[name]
	}

	override fun declareValue(value: VariableValueDeclaration) {
		declaredValues[value.name] = value
	}

	override fun resolveReference(name: String): VariableValueDeclaration? {
		return declaredValues[name]
	}
}