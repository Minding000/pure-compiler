package linter.elements.literals

import linter.Linter
import linter.elements.definitions.FunctionDefinition
import linter.elements.definitions.TypeAlias
import linter.elements.values.TypeDefinition
import linter.messages.Message
import linter.scopes.Scope
import linter.scopes.TypeScope
import parsing.ast.general.Element
import java.util.LinkedList

class SimpleType(val source: Element, val genericParameters: List<Type>, val name: String): Type() {
	var definition: TypeDefinition? = null

	constructor(definition: TypeDefinition): this(definition.source, LinkedList(), definition.name) {
		this.definition = definition
	}

	constructor(linter: Linter, definition: TypeDefinition): this(definition.source, LinkedList(), definition.name) {
		this.definition = definition
		if(linter.hasCompleted(Linter.Phase.TYPE_LINKING))
			addScope(linter, definition.scope)
	}

	init {
		units.addAll(genericParameters)
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		if(definition == null) {
			definition = scope.resolveType(name)
			if(definition == null)
				linter.messages.add(Message("${source.getStartString()}: Type '$name' hasn't been declared yet.", Message.Type.ERROR))
		}
		definition?.let {
			addScope(linter, it.scope)
		}
	}

	private fun addScope(linter: Linter, scope: TypeScope) {
		definition?.let {
			val genericTypes = it.scope.getGenericTypes()
			if(genericTypes.size == genericParameters.size) {
				for(i in genericTypes.indices)
					this.scope.genericTypes[genericTypes[i]] = genericParameters[i]
			} else {
				linter.messages.add(Message(
					"${source.getStartString()}: Number of provided generic parameters " +
							"(${genericParameters.size}) doesn't match declared number of declared " +
							"generic parameters (${genericTypes.size})", Message.Type.ERROR))
			}
			for((name, type) in scope.types)
				this.scope.types[name] = type
			for((name, value) in scope.values)
				this.scope.values[name] = value
			for(initializer in scope.initializers)
				this.scope.initializers.add(initializer)
			for((name, originalFunctions) in scope.functions) {
				val functions = LinkedList<FunctionDefinition>()
				for(function in originalFunctions)
					functions.add(function)
				this.scope.functions[name] = functions
			}
			for(operator in scope.operators)
				this.scope.operators.add(operator)
		}
	}

	override fun accepts(sourceType: Type): Boolean {
		return sourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(targetType: Type): Boolean {
		if(targetType is LambdaFunctionType)
			return false
		if(targetType !is SimpleType)
			return targetType.accepts(this)
		if(equals(targetType))
			return true
		return definition?.superType?.isAssignableTo(targetType) ?: false
	}

	override fun getKeyType(linter: Linter): Type? {
		if(genericParameters.size != 2) {
			linter.messages.add(Message("Type '$this' doesn't have a key type.", Message.Type.ERROR))
			return null
		}
		return genericParameters.first()
	}

	override fun getValueType(linter: Linter): Type? {
		if(!(genericParameters.size == 1 || genericParameters.size == 2)) {
			linter.messages.add(Message("Type '$this' doesn't have a value type.", Message.Type.ERROR))
			return null
		}
		return genericParameters.last()
	}

	override fun equals(other: Any?): Boolean {
		if(other !is SimpleType)
			return false
		if(definition != other.definition)
			return false
		if(genericParameters.size != other.genericParameters.size)
			return false
		for(i in genericParameters.indices)
			if(genericParameters[i] == other.genericParameters[i])
				return false
		return true
	}

	override fun hashCode(): Int {
		var result = genericParameters.hashCode()
		result = 31 * result + (definition?.hashCode() ?: 0)
		return result
	}

	override fun toString(): String {
		return name
	}
}