package components.semantic_analysis.semantic_model.definitions

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmType
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.context.Context
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError
import logger.issues.initialization.UninitializedProperties
import logger.issues.modifiers.*
import util.combine
import util.stringifyTypes
import java.util.*

class InitializerDefinition(override val source: SyntaxTreeNode, override val scope: BlockScope,
							val typeParameters: List<TypeDefinition> = emptyList(), val parameters: List<Parameter> = emptyList(),
							val body: SemanticModel? = null, override val isAbstract: Boolean = false, val isConverting: Boolean = false,
							val isNative: Boolean = false, val isOverriding: Boolean = false):
	SemanticModel(source, scope), MemberDeclaration, Callable {
	override lateinit var parentDefinition: TypeDefinition
	override val memberIdentifier
		get() = toString(true)
	var superInitializer: InitializerDefinition? = null
	override val propertiesRequiredToBeInitialized = LinkedList<PropertyDeclaration>()
	override val propertiesBeingInitialized = LinkedList<PropertyDeclaration>()
	lateinit var llvmValue: LlvmValue
	lateinit var llvmType: LlvmType

	init {
		addSemanticModels(typeParameters, parameters)
		addSemanticModels(body)
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
		val initializerDefinition = InitializerDefinition(source, scope, specificTypeParameters, specificParameters, body, isAbstract,
			isConverting, isNative, isOverriding)
		initializerDefinition.parentDefinition = parentDefinition
		return initializerDefinition
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
			val requiredType = genericParameter.getLinkedSuperType()
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
			val requiredType = typeParameter.getLinkedSuperType()
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
		return inferredTypes.combine(this)
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

	override fun determineTypes() {
		parentDefinition = scope.getSurroundingDefinition()
			?: throw CompilerError(source, "Initializer expected surrounding type definition.")
		super.determineTypes()
		parentDefinition.scope.declareInitializer(this)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		if(isNative)
			return
		val propertiesToBeInitialized = parentDefinition.scope.getPropertiesToBeInitialized().toMutableList()
		val initializerTracker = VariableTracker(context, true)
		for(member in parentDefinition.scope.memberDeclarations)
			if(member is PropertyDeclaration)
				initializerTracker.declare(member, member.type is StaticType)
		for(parameter in parameters)
			parameter.analyseDataFlow(initializerTracker)
		body?.analyseDataFlow(initializerTracker)
		initializerTracker.calculateEndState()
		initializerTracker.validate()
		propertiesBeingInitialized.addAll(initializerTracker.getPropertiesBeingInitialized())
		propertiesRequiredToBeInitialized.addAll(initializerTracker.getPropertiesRequiredToBeInitialized())
		tracker.addChild("${parentDefinition.name}.${memberIdentifier}", initializerTracker)
		propertiesToBeInitialized.removeAll(propertiesBeingInitialized)
		if(propertiesToBeInitialized.isNotEmpty())
			context.addIssue(UninitializedProperties(source, propertiesToBeInitialized))
	}

	override fun validate() {
		super.validate()
		if(isConverting) {
			if(typeParameters.isNotEmpty())
				context.addIssue(ConvertingInitializerTakingTypeParameters(source))
			if(parameters.size != 1)
				context.addIssue(ConvertingInitializerWithInvalidParameterCount(source))
		} else {
			if(superInitializer?.isConverting == true)
				context.addIssue(OverridingInitializerMissingConvertingKeyword(source))
		}
		val superInitializer = superInitializer
		if(superInitializer == null) {
			if(isOverriding)
				context.addIssue(OverriddenSuperInitializerMissing(source))
		} else {
			if(superInitializer.isAbstract) {
				if(!isOverriding)
					context.addIssue(MissingOverridingKeyword(source, "Initializer", toString()))
			} else {
				if(isOverriding)
					context.addIssue(OverriddenSuperInitializerMissing(source))
			}
		}
	}

	override fun declare(constructor: LlvmConstructor) {
		super.declare(constructor)
		for(index in parameters.indices)
			parameters[index].index = index
		val parameterTypes = parameters.map { parameter -> parameter.type?.getLlvmType(constructor) }
		llvmType = constructor.buildFunctionType(parameterTypes, constructor.createPointerType(parentDefinition.llvmType))
		llvmValue = constructor.buildFunction("${parentDefinition.name}_Initializer", llvmType)
	}

	override fun compile(constructor: LlvmConstructor) {
		val previousBlock = constructor.getCurrentBlock()
		constructor.createAndSelectBlock(llvmValue, "entrypoint")
		val thisValue = constructor.buildHeapAllocation(parentDefinition.llvmType, "this")
		val parentDefinition = parentDefinition
		val classDefinitionPointer = constructor.buildGetPropertyPointer(parentDefinition.llvmType, thisValue, Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinitionPointer")
		constructor.buildStore(parentDefinition.llvmClassDefinitionAddress, classDefinitionPointer)
		for(memberDeclaration in parentDefinition.properties) {
			val memberValue = memberDeclaration.value
			if(memberValue != null) {
				val memberAddress = context.resolveMember(constructor, parentDefinition.llvmType, thisValue, memberDeclaration.name)
				constructor.buildStore(memberValue.getLlvmValue(constructor), memberAddress)
			}
		}
		super.compile(constructor)
		constructor.buildReturn(thisValue)
		constructor.select(previousBlock)
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
