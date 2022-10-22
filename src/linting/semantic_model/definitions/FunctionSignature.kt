package linting.semantic_model.definitions

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.scopes.Scope
import linting.semantic_model.types.ObjectType
import linting.semantic_model.types.Type
import linting.semantic_model.values.Value
import parsing.syntax_tree.general.Element
import util.getCommonType
import java.util.*

class FunctionSignature(override val source: Element, val genericParameters: List<TypeDefinition>,
						val parameterTypes: List<Type?>, returnType: Type?,
						isPartOfImplementation: Boolean = false): Unit(source) {
	val returnType = returnType ?: ObjectType(source, Linter.LiteralType.NOTHING.className)
	var superFunctionSignature: FunctionSignature? = null

	init {
		if(!isPartOfImplementation) {
			units.addAll(genericParameters)
			for(type in parameterTypes)
				if(type != null)
					units.add(type)
			units.add(this.returnType)
		}
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

	fun getTypeSubstitutions(suppliedTypes: List<Type>, suppliedValues: List<Value>): Map<ObjectType, Type>? {
		if(genericParameters.size < suppliedTypes.size)
			return null
		if(parameterTypes.size != suppliedValues.size)
			return null
		val typeSubstitutions = HashMap<ObjectType, Type>()
		for(parameterIndex in genericParameters.indices) {
			val genericParameter = genericParameters[parameterIndex]
			val requiredType = genericParameter.superType
			val suppliedType = suppliedTypes.getOrNull(parameterIndex)
				?: inferTypeParameter(genericParameter, suppliedValues)
				?: return null
			if(requiredType?.accepts(suppliedType) == false)
				return null
			typeSubstitutions[ObjectType(genericParameter)] = suppliedType
		}
		return typeSubstitutions
	}

	private fun inferTypeParameter(typeParameter: TypeDefinition, suppliedValues: List<Value>): Type? {
		val inferredTypes = HashSet<Type>()
		for(parameterIndex in parameterTypes.indices) {
			val valueParameterType = parameterTypes[parameterIndex]
			val suppliedType = suppliedValues[parameterIndex].type ?: continue
			valueParameterType?.inferType(typeParameter, suppliedType, inferredTypes)
		}
		return inferredTypes.getCommonType(source)
	}

	fun isMoreSpecificThan(otherSignature: FunctionSignature): Boolean {
		if(otherSignature.parameterTypes.size != parameterTypes.size)
			return false
		var areSignaturesEqual = true
		for(parameterIndex in parameterTypes.indices) {
			val parameterType = parameterTypes[parameterIndex] ?: return false
			val otherParameterType = otherSignature.parameterTypes[parameterIndex]
			if(otherParameterType == null) {
				areSignaturesEqual = false
				continue
			}
			if(otherParameterType != parameterType) {
				areSignaturesEqual = false
				if(!otherParameterType.accepts(parameterType))
					return false
			}
		}
		return !areSignaturesEqual
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
