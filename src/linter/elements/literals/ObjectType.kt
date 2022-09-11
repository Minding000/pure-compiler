package linter.elements.literals

import linter.Linter
import linter.elements.definitions.OperatorDefinition
import linter.elements.definitions.TypeAlias
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration
import linter.messages.Message
import linter.scopes.Scope
import parsing.ast.general.Element
import java.util.LinkedList

class ObjectType(val source: Element, val name: String, val genericParameters: List<Type> = listOf()): Type() {
	var definition: TypeDefinition? = null
		set(value) {
			value?.scope?.subscribe(this)
			field = value
		}

	constructor(definition: TypeDefinition): this(definition.source, definition.name) {
		this.definition = definition
	}

	constructor(genericParameters: List<Type>, definition: TypeDefinition):
			this(definition.source, definition.name, genericParameters) {
		this.definition = definition
	}

	init {
		units.addAll(genericParameters)
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): Type {
		val substituteType = typeSubstitution[this]
		if(substituteType != null)
			return substituteType
		val specificGenericParameters = LinkedList<Type>()
		for(genericParameter in genericParameters)
			specificGenericParameters.add(genericParameter.withTypeSubstitutions(typeSubstitution))
		val specificType = ObjectType(source, name, specificGenericParameters)
		specificType.definition = definition
		return specificType
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
			definition = scope.resolveType(name)
			if(definition == null)
				linter.messages.add(Message(
					"${source.getStartString()}: Type '$name' hasn't been declared yet.", Message.Type.ERROR))
		}
	}

	fun acceptsSubstituteType(substituteType: Type): Boolean {
		return definition?.superType?.accepts(substituteType) ?: true
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		(definition as? TypeAlias)?.let { typeAlias ->
			return unresolvedSourceType.isAssignableTo(typeAlias.referenceType)
		}
		return unresolvedSourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = resolveTypeAlias(unresolvedTargetType)
		(definition as? TypeAlias)?.let { typeAlias ->
			return typeAlias.referenceType.isAssignableTo(targetType)
		}
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