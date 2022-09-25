package linting.semantic_model.scopes

import linting.Linter
import linting.semantic_model.definitions.IndexOperatorDefinition
import linting.semantic_model.definitions.OperatorDefinition
import linting.semantic_model.literals.Type
import linting.semantic_model.definitions.TypeDefinition
import linting.semantic_model.values.VariableValueDeclaration
import messages.Message
import kotlin.collections.HashMap

class FileScope: MutableScope() {
	private val referencedTypes = HashMap<String, TypeDefinition>()
	val types = HashMap<String, TypeDefinition>()
	private val referencedValues = HashMap<String, VariableValueDeclaration>()
	val values = HashMap<String, VariableValueDeclaration>()

	fun reference(scope: FileScope) {
		referencedTypes.putAll(scope.types)
		referencedValues.putAll(scope.values)
	}

	override fun subscribe(type: Type) {
		super.subscribe(type)
		for((_, typeDefinition) in types)
			type.onNewType(typeDefinition)
		for((_, value) in values)
			type.onNewValue(value)
	}

	override fun declareType(linter: Linter, type: TypeDefinition) {
		val previousDeclaration = referencedTypes[type.name] ?: types.putIfAbsent(type.name, type)
		if(previousDeclaration == null) {
			onNewType(type)
			linter.addMessage(type.source, "Declaration of type '${type.name}'.",
				Message.Type.DEBUG)
		} else {
			linter.addMessage(type.source, "Redeclaration of type '${type.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR)
		}
	}

	override fun resolveType(name: String): TypeDefinition? {
		return types[name] ?: referencedTypes[name]
	}

	override fun declareValue(linter: Linter, value: VariableValueDeclaration) {
		val previousDeclaration = referencedValues[value.name] ?: values.putIfAbsent(value.name, value)
		if(previousDeclaration == null) {
			onNewValue(value)
			linter.addMessage(value.source, "Declaration of value '${value.name}'.",
				Message.Type.DEBUG)
		} else {
			linter.addMessage(value.source, "Redeclaration of value '${value.name}'," +
					" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR)
		}
	}

	override fun resolveValue(name: String): VariableValueDeclaration? {
		return values[name] ?: referencedValues[name]
	}

	override fun resolveOperator(name: String, suppliedTypes: List<Type?>): OperatorDefinition? {
		return null
	}

	override fun resolveIndexOperator(suppliedIndexTypes: List<Type?>, suppliedParameterTypes: List<Type?>):
			IndexOperatorDefinition? {
		return null
	}
}