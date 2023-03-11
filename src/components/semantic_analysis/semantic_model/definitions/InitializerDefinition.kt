package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.types.OrUnionType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.Element
import messages.Message
import util.stringifyTypes
import java.util.*

class InitializerDefinition(override val source: Element, override val parentDefinition: TypeDefinition, override val scope: BlockScope,
							val typeParameters: List<TypeDefinition> = listOf(), val parameters: List<Parameter> = listOf(),
							val body: Unit? = null, override val isAbstract: Boolean = false, val isConverting: Boolean = false,
							val isNative: Boolean = false): Unit(source, scope), MemberDeclaration, Callable {
	override val memberIdentifier
		get() = toString(true)
	var superInitializer: InitializerDefinition? = null
	override val propertiesRequiredToBeInitialized = LinkedList<PropertyDeclaration>()
	override val propertiesBeingInitialized = LinkedList<PropertyDeclaration>()

	init {
		addUnits(typeParameters, parameters)
		addUnits(body)
	}

	fun withTypeSubstitutions(typeSubstitution: Map<TypeDefinition, Type>): InitializerDefinition {
		val specificTypeParameters = LinkedList<TypeDefinition>()
		for(typeParameter in typeParameters) {
			typeParameter.withTypeSubstitutions(typeSubstitution) { specificDefinition ->
				specificTypeParameters.add(specificDefinition)
			}
		}
		val specificParameters = LinkedList<Parameter>()
		for(parameter in parameters)
			specificParameters.add(parameter.withTypeSubstitutions(typeSubstitution))
		return InitializerDefinition(source, parentDefinition, scope, specificTypeParameters, specificParameters, body, isAbstract,
			isConverting, isNative)
	}

	fun fulfillsInheritanceRequirementsOf(superInitializer: InitializerDefinition): Boolean {
		if(parameters.size != superInitializer.parameters.size)
			return false
		for(parameterIndex in parameters.indices) {
			val superParameterType = superInitializer.parameters[parameterIndex].type ?: continue
			val baseParameterType = parameters[parameterIndex].type ?: continue
			if(!baseParameterType.accepts(superParameterType))
				return false
		}
		return true
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
		if(typeParameters.size < suppliedTypes.size)
			return null
		if(parameters.size != suppliedValues.size)
			return null
		val typeSubstitutions = HashMap<TypeDefinition, Type>()
		for(parameterIndex in typeParameters.indices) {
			val typeParameter = typeParameters[parameterIndex]
			val requiredType = typeParameter.superType
			val suppliedType = suppliedTypes.getOrNull(parameterIndex)
				?: inferTypeParameter(typeParameter, suppliedValues)
				?: return null
			if(requiredType?.accepts(suppliedType) == false)
				return null
			typeSubstitutions[typeParameter] = suppliedType
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
		return OrUnionType(source, scope, inferredTypes).simplified()
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

	fun isConvertingFrom(sourceType: Type): Boolean {
		return isConverting && parameters.size == 1 && parameters.first().type?.accepts(sourceType) ?: false
	}

	override fun linkPropertyParameters(linter: Linter) {
		super.linkPropertyParameters(linter)
		parentDefinition.scope.declareInitializer(linter, this)
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		val propertiesToBeInitialized = parentDefinition.scope.getPropertiesToBeInitialized().toMutableList()
		val initializerTracker = VariableTracker(true)
		for(member in parentDefinition.scope.memberDeclarations)
			if(member is PropertyDeclaration)
				initializerTracker.declare(member)
		for(parameter in parameters)
			parameter.analyseDataFlow(linter, initializerTracker)
		body?.analyseDataFlow(linter, initializerTracker)
		initializerTracker.calculateEndState()
		initializerTracker.validate(linter)
		propertiesBeingInitialized.addAll(initializerTracker.getPropertiesBeingInitialized())
		propertiesRequiredToBeInitialized.addAll(initializerTracker.getPropertiesRequiredToBeInitialized())
		tracker.addChild("${parentDefinition.name}.${memberIdentifier}", initializerTracker)
		propertiesToBeInitialized.removeAll(propertiesBeingInitialized)
		if(propertiesToBeInitialized.isNotEmpty()) {
			var message = "The following properties have not been initialized by this initializer:"
			for(uninitializedProperty in propertiesToBeInitialized)
				message += "\n - ${uninitializedProperty.memberIdentifier}"
			linter.addMessage(source, message, Message.Type.ERROR)
		}
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		if(isConverting) {
			if(typeParameters.isNotEmpty())
				linter.addMessage(source, "Converting initializers cannot take type parameters.", Message.Type.WARNING)
			if(parameters.size != 1)
				linter.addMessage(source, "Converting initializers have to take exactly one parameter.", Message.Type.WARNING)
		} else {
			if(superInitializer?.isConverting == true)
				linter.addMessage(source, "Overriding initializer of converting initializer needs to be converting.",
					Message.Type.ERROR)
		}
	}

	override fun toString(): String {
		return toString(false)
	}

	fun toString(isInternal: Boolean): String {
		var stringRepresentation = ""
		val genericTypeDefinitions = parentDefinition.scope.getGenericTypeDefinitions()
		if(genericTypeDefinitions.isNotEmpty())
			stringRepresentation += "<${genericTypeDefinitions.joinToString()}>"
		stringRepresentation += if(isInternal) "init" else parentDefinition.name
		stringRepresentation += "("
		if(typeParameters.isNotEmpty()) {
			stringRepresentation += typeParameters.joinToString()
			stringRepresentation += ";"
			if(parameters.isNotEmpty())
				stringRepresentation += " "
		}
		stringRepresentation += parameters.stringifyTypes()
		stringRepresentation += ")"
		return stringRepresentation
	}
}
