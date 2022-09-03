package linter.elements.literals

import errors.internal.CompilerError
import linter.Linter
import linter.elements.definitions.OperatorDefinition
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration
import linter.messages.Message
import linter.scopes.Scope
import parsing.ast.general.Element
import java.util.LinkedList

class ObjectType(val source: Element, val genericParameters: List<Type>, val name: String): Type() {
	var definition: TypeDefinition? = null

	constructor(definition: TypeDefinition): this(definition.source, LinkedList(), definition.name) {
		setDefinition(null, definition)
	}

	constructor(linter: Linter, genericParameters: List<Type>, definition: TypeDefinition):
			this(definition.source, genericParameters, definition.name) {
		setDefinition(linter, definition)
	}

	init {
		units.addAll(genericParameters)
	}

	private fun setDefinition(linter: Linter?, definition: TypeDefinition?) {
		this.definition = definition
		definition?.let {
			val genericTypes = it.scope.getGenericTypes()
			if(genericTypes.size == genericParameters.size) {
				for(i in genericTypes.indices)
					this.scope.genericTypes[genericTypes[i]] = genericParameters[i]
			} else {
				if(linter == null)
					throw CompilerError("Invalid condition: Unable to log linter message.")
				linter.messages.add(Message(
					"${source.getStartString()}: Number of provided generic parameters " +
							"(${genericParameters.size}) doesn't match declared number of declared " +
							"generic parameters (${genericTypes.size})", Message.Type.ERROR))
			}
			it.scope.subscribe(this)
		}
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<Type, Type>): Type {
		val specificGenericParameters = LinkedList<Type>()
		for(genericParameter in genericParameters)
			specificGenericParameters.add(typeSubstitution[genericParameter] ?: genericParameter)
		return ObjectType(source, specificGenericParameters, name)
	}

	override fun onNewType(type: TypeDefinition) {
		this.scope.addType(type)
	}

	override fun onNewValue(value: VariableValueDeclaration) {
		this.scope.addValue(value)
	}

	override fun onNewOperator(operator: OperatorDefinition) {
		this.scope.addOperator(operator)
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		if(definition == null) {
			setDefinition(linter, scope.resolveType(name))
			if(definition == null)
				linter.messages.add(Message(
					"${source.getStartString()}: Type '$name' hasn't been declared yet.", Message.Type.ERROR))
		}
	}

	override fun accepts(sourceType: Type): Boolean {
		return sourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(targetType: Type): Boolean {
		if(targetType is FunctionType)
			return false
		if(targetType !is ObjectType)
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
		if(other !is ObjectType)
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
		if(genericParameters.isEmpty())
			return name
		return genericParameters.joinToString(", ", "<", ">$name")
	}
}