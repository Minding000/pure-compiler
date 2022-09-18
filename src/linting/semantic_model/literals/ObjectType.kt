package linting.semantic_model.literals

import linting.Linter
import linting.semantic_model.definitions.OperatorDefinition
import linting.semantic_model.definitions.TypeAlias
import linting.semantic_model.values.TypeDefinition
import linting.semantic_model.values.VariableValueDeclaration
import messages.Message
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.general.Element
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
				linter.addMessage(source, "Type '$name' hasn't been declared yet.", Message.Type.ERROR)
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
			linter.addMessage("Type '$this' doesn't have a key type.", Message.Type.ERROR)
			return null
		}
		return genericParameters.first()
	}

	override fun getValueType(linter: Linter): Type? {
		if(!(genericParameters.size == 1 || genericParameters.size == 2)) {
			linter.addMessage("Type '$this' doesn't have a value type.", Message.Type.ERROR)
			return null
		}
		return genericParameters.last()
	}

	override fun equals(other: Any?): Boolean {
		if(other !is Type)
			return false
		val otherType = resolveTypeAlias(other)
		(definition as? TypeAlias)?.let { typeAlias ->
			return typeAlias.referenceType == otherType
		}
		if(otherType !is ObjectType)
			return false
		if(definition != otherType.definition)
			return false
		if(genericParameters.size != otherType.genericParameters.size)
			return false
		for(i in genericParameters.indices)
			if(genericParameters[i] == otherType.genericParameters[i])
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