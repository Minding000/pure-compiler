package linting.semantic_model.definitions

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.literals.Type
import linting.semantic_model.scopes.Scope
import linting.semantic_model.values.Value
import parsing.syntax_tree.general.Element
import java.util.*

class FunctionSignature(val source: Element, val genericParameters: List<TypeDefinition>,
						val parameterTypes: List<Type?>, returnType: Type?): Unit() {
	val returnType = returnType ?: ObjectType(source, Linter.LiteralType.NOTHING.className)
	var superFunctionSignature: FunctionSignature? = null

	init { //TODO these are already part of the unit tree (added by FunctionImplementation) and will therefore receive events twice
		units.addAll(genericParameters)
		for(type in parameterTypes)
			if(type != null)
				units.add(type)
		units.add(this.returnType)
	}

	fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): FunctionSignature {
		val specificGenericParameters = LinkedList<TypeDefinition>()
		for(genericParameter in genericParameters)
			specificGenericParameters.add(genericParameter.withTypeSubstitutions(typeSubstitution))
		val specificParametersTypes = LinkedList<Type?>()
		for(parameterType in parameterTypes)
			specificParametersTypes.add(parameterType?.withTypeSubstitutions(typeSubstitution))
		return FunctionSignature(source, specificGenericParameters, specificParametersTypes,
				returnType.withTypeSubstitutions(typeSubstitution))
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		if(Linter.LiteralType.NOTHING.matches(returnType)) {
			for(unit in units)
				if(unit != returnType)
					unit.linkTypes(linter, scope)
			linter.link(Linter.LiteralType.NOTHING, returnType)
		} else {
			super.linkTypes(linter, scope)
		}
	}

	fun accepts(suppliedValues: List<Value>): Boolean {
		if(parameterTypes.size != suppliedValues.size)
			return false
		for(parameterIndex in parameterTypes.indices)
			if(!suppliedValues[parameterIndex].isAssignableTo(parameterTypes[parameterIndex]))
				return false
		return true
	}

	fun isMoreSpecificThan(otherSignature: FunctionSignature): Boolean {
		if(parameterTypes.size != otherSignature.parameterTypes.size)
			return false
		if(otherSignature.parameterTypes == parameterTypes)
			return false
		for(parameterIndex in parameterTypes.indices) {
			val parameterType = parameterTypes[parameterIndex] ?: return false
			val otherParameterType = otherSignature.parameterTypes[parameterIndex] ?: continue
			if(!otherParameterType.accepts(parameterType))
				return false
		}
		return true
	}

	override fun equals(other: Any?): Boolean {
		if(other !is FunctionSignature)
			return false
		if(returnType != other.returnType)
			return false
		if(parameterTypes.size != other.parameterTypes.size)
			return false
		for(parameterIndex in parameterTypes.indices)
			if(parameterTypes[parameterIndex] != other.parameterTypes[parameterIndex])
				return false
		return true
	}

	override fun hashCode(): Int {
		var result = genericParameters.hashCode()
		result = 31 * result + parameterTypes.hashCode()
		result = 31 * result + returnType.hashCode()
		return result
	}

	override fun toString(): String {
		return toString(true)
	}

	fun toString(useLambdaStyle: Boolean): String {
		return if(useLambdaStyle)
			"(${parameterTypes.joinToString()}) =>${if(Linter.LiteralType.NOTHING.matches(returnType)) "|" else " $returnType"}"
		else
			"(${parameterTypes.joinToString()})${if(Linter.LiteralType.NOTHING.matches(returnType)) "" else ": $returnType"}"
	}
}