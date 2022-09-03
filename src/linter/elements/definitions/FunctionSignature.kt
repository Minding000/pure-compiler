package linter.elements.definitions

import linter.elements.general.Unit
import linter.elements.literals.Type
import linter.elements.values.TypeDefinition
import linter.scopes.InterfaceScope
import parsing.ast.general.Element
import java.util.*

class FunctionSignature(val source: Element, val genericParameters: List<TypeDefinition>,
						val parameterTypes: List<Type?>, val returnType: Type?): Unit() {

	init {
		units.addAll(genericParameters)
		for(type in parameterTypes)
			if(type != null)
				units.add(type)
		if(returnType != null)
			units.add(returnType)
	}

	fun withTypeSubstitutions(typeSubstitution: Map<Type, Type>): FunctionSignature {
		val specificGenericParameters = LinkedList<TypeDefinition>()
		for(genericParameter in genericParameters)
			specificGenericParameters.add(genericParameter.withTypeSubstitutions(typeSubstitution))
		val specificParametersTypes = LinkedList<Type?>()
		for(parameterType in parameterTypes)
			specificParametersTypes.add(parameterType?.withTypeSubstitutions(typeSubstitution))
		return FunctionSignature(source, specificGenericParameters, specificParametersTypes,
				returnType?.withTypeSubstitutions(typeSubstitution))
	}

	fun accepts(scope: InterfaceScope, types: List<Type?>): Boolean {
		if(parameterTypes.size != types.size)
			return false
		for(i in parameterTypes.indices)
			if(types[i]?.let { parameterTypes[i]?.accepts(it) } != true)
				return false
		return true
	}

	override fun equals(other: Any?): Boolean {
		if(other !is FunctionSignature)
			return false
		if(returnType != other.returnType)
			return false
		if(parameterTypes.size != other.parameterTypes.size)
			return false
		for(i in parameterTypes.indices)
			if(parameterTypes[i] != other.parameterTypes[i])
				return false
		return true
	}

	override fun hashCode(): Int {
		var result = parameterTypes.hashCode()
		result = 31 * result + (returnType?.hashCode() ?: 0)
		return result
	}

	override fun toString(): String {
		return "(${parameterTypes.joinToString()}) =>${if(returnType == null) "|" else " $returnType"}"
	}
}