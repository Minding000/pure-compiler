package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import messages.Message

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
			linter.addMessage(type.source, "Redeclaration of type '${type.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR)
			return
		}
		onNewType(type)
		linter.addMessage(type.source, "Declaration of type '${type.name}'.", Message.Type.DEBUG)
	}

	override fun resolveType(name: String): TypeDefinition? {
		return types[name] ?: referencedTypes[name]
	}

	override fun declareValue(linter: Linter, value: ValueDeclaration) {
		val previousDeclaration = referencedValues[value.name] ?: values.putIfAbsent(value.name, value)
		if(previousDeclaration != null) {
			linter.addMessage(value.source, "Redeclaration of value '${value.name}'," +
					" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR)
			return
		}
		linter.addMessage(value.source, "Declaration of value '${value.name}'.",
			Message.Type.DEBUG)
	}

	override fun resolveValue(name: String): ValueDeclaration? {
		return values[name] ?: referencedValues[name]
	}
}
