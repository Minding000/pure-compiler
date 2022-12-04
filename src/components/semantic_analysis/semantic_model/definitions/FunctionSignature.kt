package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.Element
import util.getCommonType
import java.util.*

class FunctionSignature(override val source: Element, val genericParameters: List<TypeDefinition>,
						val parameterTypes: List<Type?>, returnType: Type?,
						isPartOfImplementation: Boolean = false): Unit(source) {
	val returnType = returnType ?: ObjectType(source, Linter.LiteralType.NOTHING.className)
	var superFunctionSignature: FunctionSignature? = null

	init {
		if(!isPartOfImplementation) {
			addUnits(this.returnType)
			addUnits(genericParameters, parameterTypes)
		}
	}

	fun withTypeSubstitutions(typeSubstitution: Map<TypeDefinition, Type>): FunctionSignature {
		val specificGenericParameters = LinkedList<TypeDefinition>()
		for(genericParameter in genericParameters) {
			genericParameter.withTypeSubstitutions(typeSubstitution) { specificDefinition ->
				specificGenericParameters.add(specificDefinition)
			}
		}
		val specificParametersTypes = LinkedList<Type?>()
		for(parameterType in parameterTypes)
			specificParametersTypes.add(parameterType?.withTypeSubstitutions(typeSubstitution))
		return FunctionSignature(source, specificGenericParameters, specificParametersTypes,
				returnType.withTypeSubstitutions(typeSubstitution))
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		for(unit in units) {
			if(Linter.LiteralType.NOTHING.matches(unit)) {
				linter.link(Linter.LiteralType.NOTHING, unit)
				continue
			}
			unit.linkTypes(linter, scope)
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

	fun getTypeSubstitutions(suppliedTypes: List<Type>, suppliedValues: List<Value>): Map<TypeDefinition, Type>? {
		if(genericParameters.size < suppliedTypes.size)
			return null
		if(parameterTypes.size != suppliedValues.size)
			return null
		val typeSubstitutions = HashMap<TypeDefinition, Type>()
		for(parameterIndex in genericParameters.indices) {
			val genericParameter = genericParameters[parameterIndex]
			val requiredType = genericParameter.superType
			val suppliedType = suppliedTypes.getOrNull(parameterIndex)
				?: inferTypeParameter(genericParameter, suppliedValues)
				?: return null
			if(requiredType?.accepts(suppliedType) == false)
				return null
			typeSubstitutions[genericParameter] = suppliedType
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

	fun fulfillsInheritanceRequirementOf(superSignature: FunctionSignature): Boolean {
		if(returnType.isAssignableTo(superSignature.returnType))
			return false
		if(parameterTypes.size != superSignature.parameterTypes.size)
			return false
		for(parameterIndex in parameterTypes.indices)
			if(superSignature.parameterTypes[parameterIndex]?.let {
						superParameterType -> parameterTypes[parameterIndex]?.accepts(superParameterType) } == false)
				return false
		return true
	}

	fun hasSameParameterTypesAs(otherSignature: FunctionSignature): Boolean {
		if(parameterTypes.size != otherSignature.parameterTypes.size)
			return false
		for(parameterIndex in parameterTypes.indices)
			if(parameterTypes[parameterIndex] != otherSignature.parameterTypes[parameterIndex])
				return false
		return true
	}

	fun accepts(other: FunctionSignature): Boolean {
		if(other.parameterTypes.size != parameterTypes.size)
			return false
		for(parameterIndex in parameterTypes.indices) {
			if(parameterTypes[parameterIndex]?.let { parameterType ->
					other.parameterTypes[parameterIndex]?.accepts(parameterType) } == false)
				return false
		}
		if(!returnType.accepts(other.returnType))
			return false
		return true
	}

	override fun equals(other: Any?): Boolean {
		if(other !is FunctionSignature)
			return false
		if(returnType != other.returnType)
			return false
		if(!this.hasSameParameterTypesAs(other))
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
		var stringRepresentation = ""
		if(genericParameters.isNotEmpty() || parameterTypes.isNotEmpty()) {
			stringRepresentation += "("
			if(genericParameters.isNotEmpty()) {
				stringRepresentation += genericParameters.joinToString()
				stringRepresentation += ";"
				if(parameterTypes.isNotEmpty())
					stringRepresentation += " "
			}
			stringRepresentation += "${parameterTypes.joinToString()})"
		}
		if(useLambdaStyle) {
			if(stringRepresentation.isNotEmpty())
				stringRepresentation += " "
			stringRepresentation += "=>"
			stringRepresentation += if(Linter.LiteralType.NOTHING.matches(returnType)) "|" else " $returnType"
		} else {
			if(!Linter.LiteralType.NOTHING.matches(returnType))
				stringRepresentation += ": $returnType"
		}
		return stringRepresentation
	}
}
