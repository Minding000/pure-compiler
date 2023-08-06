package components.semantic_analysis.semantic_model.definitions

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmType
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.context.Context
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.types.PluralType
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
import kotlin.math.max

class InitializerDefinition(override val source: SyntaxTreeNode, override val scope: BlockScope,
							val typeParameters: List<TypeDefinition> = emptyList(), val parameters: List<Parameter> = emptyList(),
							val body: SemanticModel? = null, override val isAbstract: Boolean = false, val isConverting: Boolean = false,
							val isNative: Boolean = false, val isOverriding: Boolean = false):
	SemanticModel(source, scope), MemberDeclaration, Callable {
	override lateinit var parentDefinition: TypeDefinition
	override val memberIdentifier
		get() = toString(true)
	val fixedParameters = LinkedList<Parameter>()
	var variadicParameter: Parameter? = null
	var superInitializer: InitializerDefinition? = null
	override val propertiesRequiredToBeInitialized = LinkedList<PropertyDeclaration>()
	override val propertiesBeingInitialized = LinkedList<PropertyDeclaration>()
	lateinit var llvmValue: LlvmValue
	lateinit var llvmType: LlvmType

	init {
		addSemanticModels(typeParameters, parameters)
		addSemanticModels(body)
	}

	fun getDefinitionTypeSubstitutions(genericDefinitionTypes: List<TypeDefinition>, suppliedDefinitionTypes: List<Type>,
									   suppliedValues: List<Value>): Map<TypeDefinition, Type>? {
		if(suppliedDefinitionTypes.size > genericDefinitionTypes.size)
			return null
		if(variadicParameter == null) {
			if(suppliedValues.size != fixedParameters.size)
				return null
		} else {
			if(suppliedValues.size < fixedParameters.size)
				return null
		}
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
		assert(suppliedValues.size >= fixedParameters.size)

		if(suppliedTypes.size > typeParameters.size)
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
		for(parameterIndex in suppliedValues.indices) {
			val parameterType = getParameterTypeAt(parameterIndex)
			val suppliedType = suppliedValues[parameterIndex].type ?: continue
			parameterType?.inferType(typeParameter, suppliedType, inferredTypes)
		}
		if(inferredTypes.isEmpty())
			return null
		return inferredTypes.combine(this)
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
		initializerDefinition.categorizeParameters()
		initializerDefinition.parentDefinition = parentDefinition
		return initializerDefinition
	}

	//TODO deduplicate with FunctionSignature?
	//TODO support labeled input values (same for functions)
	// -> make sure they are passed in the correct order (LLVM side)
	fun accepts(suppliedValues: List<Value>): Boolean {
		assert(suppliedValues.size >= fixedParameters.size)

		for(parameterIndex in suppliedValues.indices) {
			val parameterType = getParameterTypeAt(parameterIndex)
			if(!suppliedValues[parameterIndex].isAssignableTo(parameterType))
				return false
		}
		return true
	}

	fun isMoreSpecificThan(otherInitializerDefinition: InitializerDefinition): Boolean {
		for(parameterIndex in 0 until max(fixedParameters.size, otherInitializerDefinition.fixedParameters.size)) {
			val parameterType = getParameterTypeAt(parameterIndex) ?: return false
			val otherParameterType = otherInitializerDefinition.getParameterTypeAt(parameterIndex) ?: return true
			if(parameterType == otherParameterType)
				continue
			return otherParameterType.accepts(parameterType)
		}
		val otherVariadicParameter = otherInitializerDefinition.variadicParameter
		if(otherVariadicParameter != null) {
			if(variadicParameter == null)
				return true
			val variadicParameterType = variadicParameter?.type ?: return false
			val otherVariadicParameterType = otherVariadicParameter.type ?: return true
			if(variadicParameterType != otherVariadicParameterType)
				return otherVariadicParameterType.accepts(variadicParameterType)
		}
		return false
	}

	fun fulfillsInheritanceRequirementsOf(superInitializer: InitializerDefinition): Boolean { //TODO support variadic parameters
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

	override fun determineTypes() {
		parentDefinition = scope.getSurroundingDefinition()
			?: throw CompilerError(source, "Initializer expected surrounding type definition.")
		super.determineTypes()
		categorizeParameters()
		parentDefinition.scope.declareInitializer(this)
	}

	private fun categorizeParameters() {
		//TODO this only works with one PluralType per function for now, so that restriction should be enforced by the linter
		for(parameter in parameters) {
			if(parameter.type is PluralType) {
				variadicParameter = parameter
				continue
			}
			fixedParameters.add(parameter)
		}
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
			if(fixedParameters.size != 1)
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
		val parameterTypes = LinkedList<LlvmType?>(fixedParameters.map { parameter -> parameter.type?.getLlvmType(constructor) })
		parameterTypes.addFirst(constructor.createPointerType(parentDefinition.llvmType))
		llvmType = constructor.buildFunctionType(parameterTypes, constructor.voidType, variadicParameter != null)
		llvmValue = constructor.buildFunction("${parentDefinition.name}_Initializer", llvmType)
	}

	override fun compile(constructor: LlvmConstructor) {
		val previousBlock = constructor.getCurrentBlock()
		constructor.createAndSelectBlock(llvmValue, "entrypoint")
		val thisValue = constructor.getParameter(llvmValue, Context.THIS_PARAMETER_INDEX)
		for(memberDeclaration in parentDefinition.properties) {
			val memberValue = memberDeclaration.value
			if(memberValue != null) {
				val memberAddress = context.resolveMember(constructor, parentDefinition.llvmType, thisValue, memberDeclaration.name)
				constructor.buildStore(memberValue.getLlvmValue(constructor), memberAddress)
			}
		}
		if(body == null)
			callTrivialSuperInitializers(constructor, thisValue)
		else
			super.compile(constructor)
		constructor.buildReturn()
		constructor.select(previousBlock)
	}

	private fun callTrivialSuperInitializers(constructor: LlvmConstructor, thisValue: LlvmValue) {
		for(superType in parentDefinition.getDirectSuperTypes()) {
			if(SpecialType.ANY.matches(superType))
				continue
			val trivialInitializer = (superType.definition?.staticValueDeclaration?.type as? StaticType)?.resolveInitializer()?.signature
				?: throw CompilerError(source, "Default initializer in class '${parentDefinition.name}'" +
					" with super class '${superType.definition?.name}' without trivial initializer.")
			constructor.buildFunctionCall(trivialInitializer.llvmType, trivialInitializer.llvmValue, listOf(thisValue))
		}
	}

	fun isConvertingFrom(sourceType: Type): Boolean {
		return isConverting && fixedParameters.size == 1 && fixedParameters.first().type?.accepts(sourceType) ?: false
	}

	fun getParameterTypeAt(index: Int): Type? {
		return if(index < fixedParameters.size)
			fixedParameters[index].type
		else
			(variadicParameter?.type as? PluralType)?.baseType
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
