package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import util.getCommonType
import util.stringifyTypes
import java.util.*
import components.syntax_parser.syntax_tree.definitions.OperatorDefinition as OperatorDefinitionSyntaxTree

class IndexOperatorDefinition(source: OperatorDefinitionSyntaxTree, scope: BlockScope,
							  val genericParameters: List<TypeDefinition>, val indexParameters: List<Parameter>,
							  parameters: List<Parameter>, body: Unit?, returnType: Type?, isAbstract: Boolean,
							  isNative: Boolean, isOverriding: Boolean):
	OperatorDefinition(source, "[]", scope, parameters, body, returnType, isAbstract, isNative, isOverriding) {

	init {
		addUnits(genericParameters, indexParameters)
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<TypeDefinition, Type>): IndexOperatorDefinition {
		val specificGenericParameters = LinkedList<TypeDefinition>()
		for(genericParameter in genericParameters) {
			genericParameter.withTypeSubstitutions(typeSubstitution) { specificDefinition ->
				specificGenericParameters.add(specificDefinition)
			}
		}
		val specificIndices = LinkedList<Parameter>()
		for(index in indexParameters)
			specificIndices.add(index.withTypeSubstitutions(typeSubstitution))
		val specificParameters = LinkedList<Parameter>()
		for(parameter in valueParameters)
			specificParameters.add(parameter.withTypeSubstitutions(typeSubstitution))
		return IndexOperatorDefinition(source, scope, specificGenericParameters, specificIndices, specificParameters,
			body, returnType.withTypeSubstitutions(typeSubstitution), isAbstract, isNative, isOverriding)
	}

	fun accepts(suppliedIndexValues: List<Value>, suppliedParameterValues: List<Value>): Boolean {
		if(indexParameters.size != suppliedIndexValues.size)
			return false
		if(valueParameters.size != suppliedParameterValues.size)
			return false
		for(indexIndex in indexParameters.indices)
			if(!suppliedIndexValues[indexIndex].isAssignableTo(indexParameters[indexIndex].type))
				return false
		for(parameterIndex in valueParameters.indices)
			if(!suppliedParameterValues[parameterIndex].isAssignableTo(valueParameters[parameterIndex].type))
				return false
		return true
	}

	fun getTypeSubstitutions(suppliedTypes: List<Type>, suppliedIndexValues: List<Value>,
							 suppliedParameterValues: List<Value>): Map<TypeDefinition, Type>? {
		if(genericParameters.size < suppliedTypes.size)
			return null
		if(indexParameters.size != suppliedIndexValues.size)
			return null
		if(valueParameters.size != suppliedParameterValues.size)
			return null
		val typeSubstitutions = HashMap<TypeDefinition, Type>()
		for(parameterIndex in genericParameters.indices) {
			val genericParameter = genericParameters[parameterIndex]
			val requiredType = genericParameter.superType
			val suppliedType = suppliedTypes.getOrNull(parameterIndex)
				?: inferTypeParameter(genericParameter, suppliedIndexValues, suppliedParameterValues)
				?: return null
			if(requiredType?.accepts(suppliedType) == false)
				return null
			typeSubstitutions[genericParameter] = suppliedType
		}
		return typeSubstitutions
	}

	private fun inferTypeParameter(typeParameter: TypeDefinition, suppliedIndexValues: List<Value>,
								   suppliedParameterValues: List<Value>): Type? {
		val inferredTypes = HashSet<Type>()
		for(indexParameterIndex in indexParameters.indices) {
			val indexParameterType = indexParameters[indexParameterIndex].type
			val suppliedType = suppliedIndexValues[indexParameterIndex].type ?: continue
			indexParameterType?.inferType(typeParameter, suppliedType, inferredTypes)
		}
		for(valueParameterIndex in valueParameters.indices) {
			val valueParameterType = valueParameters[valueParameterIndex].type
			val suppliedType = suppliedParameterValues[valueParameterIndex].type ?: continue
			valueParameterType?.inferType(typeParameter, suppliedType, inferredTypes)
		}
		return inferredTypes.getCommonType(source)
	}

	fun isMoreSpecificThan(otherSignature: IndexOperatorDefinition): Boolean {
		if(otherSignature.indexParameters.size != indexParameters.size)
			return false
		if(otherSignature.valueParameters.size != valueParameters.size)
			return false
		var areSignaturesEqual = true
		for(indexParameterIndex in indexParameters.indices) {
			val indexParameterType = indexParameters[indexParameterIndex].type ?: return false
			val otherIndexParameterType = otherSignature.indexParameters[indexParameterIndex].type
			if(otherIndexParameterType == null) {
				areSignaturesEqual = false
				continue
			}
			if(otherIndexParameterType != indexParameterType) {
				areSignaturesEqual = false
				if(!otherIndexParameterType.accepts(indexParameterType))
					return false
			}
		}
		for(valueParameterIndex in valueParameters.indices) {
			val valueParameterType = valueParameters[valueParameterIndex].type ?: return false
			val otherValueParameterType = otherSignature.valueParameters[valueParameterIndex].type
			if(otherValueParameterType == null) {
				areSignaturesEqual = false
				continue
			}
			if(otherValueParameterType != valueParameterType) {
				areSignaturesEqual = false
				if(!otherValueParameterType.accepts(valueParameterType))
					return false
			}
		}
		return !areSignaturesEqual
	}

	override fun toString(): String {
		var stringRepresentation = "["
		if(genericParameters.isNotEmpty()) {
			stringRepresentation += genericParameters.joinToString()
			stringRepresentation += ";"
			if(indexParameters.isNotEmpty())
				stringRepresentation += " "
		}
		stringRepresentation += "${indexParameters.stringifyTypes()}]"
		stringRepresentation += "(${valueParameters.stringifyTypes()})"
		if(!Linter.LiteralType.NOTHING.matches(returnType))
			stringRepresentation += ": $returnType"
		return stringRepresentation
	}
}
