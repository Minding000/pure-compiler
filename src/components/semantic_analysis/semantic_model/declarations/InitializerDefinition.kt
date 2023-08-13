package components.semantic_analysis.semantic_model.declarations

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
import logger.issues.declaration.InvalidVariadicParameterPosition
import logger.issues.declaration.MultipleVariadicParameters
import logger.issues.initialization.UninitializedProperties
import logger.issues.modifiers.*
import util.combine
import util.stringifyTypes
import java.util.*
import kotlin.math.max

class InitializerDefinition(override val source: SyntaxTreeNode, override val scope: BlockScope,
							val localTypeParameters: List<TypeDeclaration> = emptyList(), val parameters: List<Parameter> = emptyList(),
							val body: SemanticModel? = null, override val isAbstract: Boolean = false, val isConverting: Boolean = false,
							val isNative: Boolean = false, val isOverriding: Boolean = false):
	SemanticModel(source, scope), MemberDeclaration, Callable {
	override lateinit var parentTypeDeclaration: TypeDeclaration
	override val memberIdentifier
		get() = toString(true)
	private val isVariadic = parameters.lastOrNull()?.isVariadic ?: false
	private val fixedParameters: List<Parameter>
	private val variadicParameter: Parameter?
	var superInitializer: InitializerDefinition? = null
	override val propertiesRequiredToBeInitialized = LinkedList<PropertyDeclaration>()
	override val propertiesBeingInitialized = LinkedList<PropertyDeclaration>()
	lateinit var llvmValue: LlvmValue
	lateinit var llvmType: LlvmType

	init {
		addSemanticModels(localTypeParameters, parameters)
		addSemanticModels(body)
		if(isVariadic) {
			this.fixedParameters = parameters.subList(0, parameters.size - 1)
			this.variadicParameter = parameters.last()
		} else {
			this.fixedParameters = parameters
			this.variadicParameter = null
		}
	}

	fun getGlobalTypeSubstitutions(globalTypeParameters: List<TypeDeclaration>, suppliedGlobalTypes: List<Type>,
								   suppliedValues: List<Value>): Map<TypeDeclaration, Type>? {
		if(suppliedGlobalTypes.size > globalTypeParameters.size)
			return null
		if(variadicParameter == null) {
			if(suppliedValues.size != fixedParameters.size)
				return null
		} else {
			if(suppliedValues.size < fixedParameters.size)
				return null
		}
		val globalTypeSubstitutions = LinkedHashMap<TypeDeclaration, Type>()
		for(parameterIndex in globalTypeParameters.indices) {
			val globalTypeParameter = globalTypeParameters[parameterIndex]
			val requiredType = globalTypeParameter.getLinkedSuperType()
			val suppliedType = suppliedGlobalTypes.getOrNull(parameterIndex)
				?: inferTypeParameter(globalTypeParameter, suppliedValues)
				?: return null
			if(requiredType?.accepts(suppliedType) == false)
				return null
			globalTypeSubstitutions[globalTypeParameter] = suppliedType
		}
		return globalTypeSubstitutions
	}

	fun getLocalTypeSubstitutions(suppliedLocalTypes: List<Type>, suppliedValues: List<Value>): Map<TypeDeclaration, Type>? {
		assert(suppliedValues.size >= fixedParameters.size)

		if(suppliedLocalTypes.size > localTypeParameters.size)
			return null
		val localTypeSubstitutions = HashMap<TypeDeclaration, Type>()
		for(parameterIndex in localTypeParameters.indices) {
			val localTypeParameter = localTypeParameters[parameterIndex]
			val requiredType = localTypeParameter.getLinkedSuperType()
			val suppliedType = suppliedLocalTypes.getOrNull(parameterIndex)
				?: inferTypeParameter(localTypeParameter, suppliedValues)
				?: return null
			if(requiredType?.accepts(suppliedType) == false)
				return null
			localTypeSubstitutions[localTypeParameter] = suppliedType
		}
		return localTypeSubstitutions
	}

	private fun inferTypeParameter(typeParameter: TypeDeclaration, suppliedValues: List<Value>): Type? {
		val inferredTypes = LinkedList<Type>()
		for(parameterIndex in suppliedValues.indices) {
			val parameterType = getParameterTypeAt(parameterIndex)
			val suppliedType = suppliedValues[parameterIndex].type ?: continue
			parameterType?.inferTypeParameter(typeParameter, suppliedType, inferredTypes)
		}
		if(inferredTypes.isEmpty())
			return null
		return inferredTypes.combine(this)
	}

	fun withTypeSubstitutions(typeSubstitution: Map<TypeDeclaration, Type>): InitializerDefinition {
		val specificLocalTypeParameters = LinkedList<TypeDeclaration>()
		for(localTypeParameter in localTypeParameters) {
			localTypeParameter.withTypeSubstitutions(typeSubstitution) { specificTypeDeclaration ->
				specificLocalTypeParameters.add(specificTypeDeclaration)
			}
		}
		val specificParameters = LinkedList<Parameter>()
		for(parameter in parameters)
			specificParameters.add(parameter.withTypeSubstitutions(typeSubstitution))
		val initializerDefinition = InitializerDefinition(source, scope, specificLocalTypeParameters, specificParameters, body, isAbstract,
			isConverting, isNative, isOverriding)
		initializerDefinition.parentTypeDeclaration = parentTypeDeclaration
		return initializerDefinition
	}

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
			if(parameterType != otherParameterType)
				return otherParameterType.accepts(parameterType)
		}
		val otherVariadicParameter = otherInitializerDefinition.variadicParameter
		if(otherVariadicParameter != null) {
			if(variadicParameter == null)
				return true
			val variadicParameterType = variadicParameter.type ?: return false
			val otherVariadicParameterType = otherVariadicParameter.type ?: return true
			if(variadicParameterType != otherVariadicParameterType)
				return otherVariadicParameterType.accepts(variadicParameterType)
		}
		return false
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

	override fun determineTypes() {
		parentTypeDeclaration = scope.getSurroundingTypeDeclaration()
			?: throw CompilerError(source, "Initializer expected surrounding type definition.")
		super.determineTypes()
		parentTypeDeclaration.scope.addInitializer(this)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		if(isNative)
			return
		val propertiesToBeInitialized = parentTypeDeclaration.scope.getPropertiesToBeInitialized().toMutableList()
		val initializerTracker = VariableTracker(context, true)
		for(member in parentTypeDeclaration.scope.memberDeclarations)
			if(member is PropertyDeclaration)
				initializerTracker.declare(member, member.type is StaticType)
		for(parameter in parameters)
			parameter.analyseDataFlow(initializerTracker)
		body?.analyseDataFlow(initializerTracker)
		initializerTracker.calculateEndState()
		initializerTracker.validate()
		propertiesBeingInitialized.addAll(initializerTracker.getPropertiesBeingInitialized())
		propertiesRequiredToBeInitialized.addAll(initializerTracker.getPropertiesRequiredToBeInitialized())
		tracker.addChild("${parentTypeDeclaration.name}.${memberIdentifier}", initializerTracker)
		propertiesToBeInitialized.removeAll(propertiesBeingInitialized)
		if(propertiesToBeInitialized.isNotEmpty())
			context.addIssue(UninitializedProperties(source, propertiesToBeInitialized))
	}

	override fun validate() {
		super.validate()
		validateConvertingKeyword()
		validateOverridingKeyword()
		validateVariadicParameter()
	}

	private fun validateConvertingKeyword() {
		if(isConverting) {
			if(localTypeParameters.isNotEmpty())
				context.addIssue(ConvertingInitializerTakingTypeParameters(source))
			if(fixedParameters.size != 1)
				context.addIssue(ConvertingInitializerWithInvalidParameterCount(source))
		} else {
			if(superInitializer?.isConverting == true)
				context.addIssue(OverridingInitializerMissingConvertingKeyword(source))
		}
	}

	private fun validateOverridingKeyword() {
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

	private fun validateVariadicParameter() { //TODO validate that variadic parameters have Collection / Plural type
		for(parameter in fixedParameters) {
			if(parameter.isVariadic) {
				if(variadicParameter == null)
					context.addIssue(InvalidVariadicParameterPosition(parameter.source))
				else
					context.addIssue(MultipleVariadicParameters(source))
			}
		}
	}

	override fun declare(constructor: LlvmConstructor) {
		super.declare(constructor)
		for(index in parameters.indices)
			parameters[index].index = index
		val parameterTypes = LinkedList<LlvmType?>(fixedParameters.map { parameter -> parameter.type?.getLlvmType(constructor) })
		parameterTypes.addFirst(constructor.pointerType)
		llvmType = constructor.buildFunctionType(parameterTypes, constructor.voidType, isVariadic)
		llvmValue = constructor.buildFunction("${parentTypeDeclaration.name}_Initializer", llvmType)
	}

	override fun compile(constructor: LlvmConstructor) {
		val previousBlock = constructor.getCurrentBlock()
		constructor.createAndSelectBlock(llvmValue, "entrypoint")
		val thisValue = constructor.getParameter(llvmValue, Context.THIS_PARAMETER_INDEX)
		for(memberDeclaration in parentTypeDeclaration.properties) {
			val memberValue = memberDeclaration.value
			if(memberValue != null) {
				val memberAddress = context.resolveMember(constructor, parentTypeDeclaration.llvmType, thisValue, memberDeclaration.name)
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
		for(superType in parentTypeDeclaration.getDirectSuperTypes()) {
			if(SpecialType.ANY.matches(superType))
				continue
			val trivialInitializer = (superType.typeDeclaration?.staticValueDeclaration?.type as? StaticType)?.getInitializer()?.initializer
				?: throw CompilerError(source, "Default initializer in class '${parentTypeDeclaration.name}'" +
					" with super class '${superType.typeDeclaration?.name}' without trivial initializer.")
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
		val genericTypeDefinitions = parentTypeDeclaration.scope.getGenericTypeDeclarations()
		if(genericTypeDefinitions.isNotEmpty())
			stringRepresentation += "<${genericTypeDefinitions.joinToString()}>"
		stringRepresentation += if(isInternal) "init" else parentTypeDeclaration.name
		stringRepresentation += "("
		if(localTypeParameters.isNotEmpty()) {
			stringRepresentation += localTypeParameters.joinToString()
			stringRepresentation += ";"
			if(parameters.isNotEmpty())
				stringRepresentation += " "
		}
		stringRepresentation += parameters.stringifyTypes()
		stringRepresentation += ")"
		return stringRepresentation
	}
}
