package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.OrUnionType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import util.stringifyTypes
import java.util.*
import components.syntax_parser.syntax_tree.definitions.InitializerDefinition as InitializerDefinitionSyntaxTree

class InitializerDefinition(override val source: InitializerDefinitionSyntaxTree,
							override val parentDefinition: TypeDefinition, val scope: BlockScope,
							val genericParameters: List<TypeDefinition>, val parameters: List<Parameter>,
							val body: Unit?, val isNative: Boolean): Unit(source), MemberDeclaration {
	override var memberIdentifier = toString()
	override val isAbstract = false

	init {
		addUnits(body)
		addUnits(genericParameters, parameters)
	}

	fun withTypeSubstitutions(typeSubstitution: Map<TypeDefinition, Type>): InitializerDefinition {
		val specificGenericParameters = LinkedList<TypeDefinition>()
		for(genericParameter in genericParameters) {
			genericParameter.withTypeSubstitutions(typeSubstitution) { specificDefinition ->
				specificGenericParameters.add(specificDefinition)
			}
		}
		val specificParameters = LinkedList<Parameter>()
		for(parameter in parameters)
			specificParameters.add(parameter.withTypeSubstitutions(typeSubstitution))
		return InitializerDefinition(source, parentDefinition, scope, specificGenericParameters, specificParameters,
			body, isNative)
	}

	fun accepts(suppliedValues: List<Value>): Boolean {
		if(parameters.size != suppliedValues.size)
			return false
		for(parameterIndex in parameters.indices) {
			if(!suppliedValues[parameterIndex].isAssignableTo(parameters[parameterIndex].type))
				return false
		}
		return true
	}

	fun getDefinitionTypeSubstitutions(genericDefinitionTypes: List<TypeDefinition>, suppliedDefinitionTypes: List<Type>,
									   suppliedValues: List<Value>): Map<TypeDefinition, Type>? {
		if(genericDefinitionTypes.size < suppliedDefinitionTypes.size)
			return null
		if(parameters.size != suppliedValues.size)
			return null
		val typeSubstitutions = LinkedHashMap<TypeDefinition, Type>()
		for(parameterIndex in genericDefinitionTypes.indices) {
			val genericParameter = genericDefinitionTypes[parameterIndex]
			val requiredType = genericParameter.superType
			val suppliedType = suppliedDefinitionTypes.getOrNull(parameterIndex)
				?: inferTypeParameter(genericParameter, suppliedValues)
				?: return null
			if(requiredType?.accepts(suppliedType) == false)
				return null
			typeSubstitutions[genericParameter] = suppliedType
		}
		return typeSubstitutions
	}

	fun getTypeSubstitutions(suppliedTypes: List<Type>, suppliedValues: List<Value>): Map<TypeDefinition, Type>? {
		if(genericParameters.size < suppliedTypes.size)
			return null
		if(parameters.size != suppliedValues.size)
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
		val inferredTypes = LinkedList<Type>()
		for(parameterIndex in parameters.indices) {
			val valueParameterType = parameters[parameterIndex].type
			val suppliedType = suppliedValues[parameterIndex].type ?: continue
			valueParameterType?.inferType(typeParameter, suppliedType, inferredTypes)
		}
		if(inferredTypes.isEmpty())
			return null
		return OrUnionType(source, inferredTypes).simplified()
	}

	fun isMoreSpecificThan(otherInitializerDefinition: InitializerDefinition): Boolean {
		if(parameters.size != otherInitializerDefinition.parameters.size)
			return false
		var hasSameSpecificity = true
		for(parameterIndex in parameters.indices) {
			val parameterType = parameters[parameterIndex].type
			val otherParameterType = otherInitializerDefinition.parameters[parameterIndex].type
			if(parameterType != otherParameterType) {
				hasSameSpecificity = false
				break
			}
		}
		if(hasSameSpecificity)
			return false
		for(parameterIndex in parameters.indices) {
			val parameterType = parameters[parameterIndex].type ?: return false
			val otherParameterType = otherInitializerDefinition.parameters[parameterIndex].type ?: continue
			if(!otherParameterType.accepts(parameterType))
				return false
		}
		return true
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, this.scope)
	}

	override fun linkPropertyParameters(linter: Linter, scope: MutableScope) {
		super.linkPropertyParameters(linter, this.scope)
		scope.declareInitializer(linter, this)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
	}

	override fun toString(): String {
		var stringRepresentation = ""
		val genericTypeDefinitions = parentDefinition.scope.getGenericTypeDefinitions()
		if(genericTypeDefinitions.isNotEmpty())
			stringRepresentation += "<${genericTypeDefinitions.joinToString()}>"
		stringRepresentation += parentDefinition.name
		stringRepresentation += "("
		if(genericParameters.isNotEmpty()) {
			stringRepresentation += genericParameters.joinToString()
			stringRepresentation += ";"
			if(parameters.isNotEmpty())
				stringRepresentation += " "
		}
		stringRepresentation += parameters.stringifyTypes()
		stringRepresentation += ")"
		return stringRepresentation
	}
}
