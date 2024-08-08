package components.semantic_model.declarations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmType
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.ComparisonResult
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.BlockScope
import components.semantic_model.types.PluralType
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError
import logger.issues.declaration.ExtraneousBody
import logger.issues.declaration.InvalidVariadicParameterPosition
import logger.issues.declaration.MultipleVariadicParameters
import logger.issues.initialization.UninitializedProperties
import logger.issues.modifiers.*
import logger.issues.resolution.ConversionAmbiguity
import util.combineOrUnion
import util.stringifyTypes
import java.util.*
import kotlin.math.max

//TODO disallow converting initializers in bound classes
class InitializerDefinition(override val source: SyntaxTreeNode, override val scope: BlockScope,
							val localTypeParameters: List<TypeDeclaration> = emptyList(), val parameters: List<Parameter> = emptyList(),
							val body: SemanticModel? = null, override val isAbstract: Boolean = false, val isConverting: Boolean = false,
							val isNative: Boolean = false, val isOverriding: Boolean = false):
	SemanticModel(source, scope), MemberDeclaration, Callable {
	override lateinit var parentTypeDeclaration: TypeDeclaration
	override val memberIdentifier
		get() = toString(true)
	val isVariadic = parameters.lastOrNull()?.isVariadic ?: false
	val fixedParameters: List<Parameter>
	private val variadicParameter: Parameter?
	var superInitializer: InitializerDefinition? = null
	private val initializerTracker = VariableTracker(context, true)
	override var hasDataFlowBeenAnalysed = isNative
	override val propertiesRequiredToBeInitialized = LinkedList<PropertyDeclaration>()
	override val propertiesBeingInitialized = LinkedList<PropertyDeclaration>()
	lateinit var llvmValue: LlvmValue
	lateinit var llvmType: LlvmType

	init {
		scope.semanticModel = this
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
		//TODO also infer type from return statement (write test!)
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

	fun getLocalTypeSubstitutions(globalTypeSubstitutions: Map<TypeDeclaration, Type>, suppliedLocalTypes: List<Type>,
								  suppliedValues: List<Value>): Map<TypeDeclaration, Type>? {
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
			val suppliedType = suppliedValues[parameterIndex].providedType ?: continue
			parameterType?.inferTypeParameter(typeParameter, suppliedType, inferredTypes)
		}
		if(inferredTypes.isEmpty())
			return null
		return inferredTypes.combineOrUnion(this)
	}

	//TODO support labeled input values (same for functions)
	// -> make sure they are passed in the correct order (LLVM side)
	fun accepts(globalTypeSubstitutions: Map<TypeDeclaration, Type>, localTypeSubstitutions: Map<TypeDeclaration, Type>,
				suppliedValues: List<Value>, conversions: MutableMap<Value, InitializerDefinition>): Boolean {
		assert(suppliedValues.size >= fixedParameters.size)

		for(parameterIndex in suppliedValues.indices) {
			val parameterType = getParameterTypeAt(parameterIndex)
				?.withTypeSubstitutions(localTypeSubstitutions)
				?.withTypeSubstitutions(globalTypeSubstitutions)
				?: return false
			val suppliedValue = suppliedValues[parameterIndex]
			if(!suppliedValue.isAssignableTo(parameterType)) {
				val suppliedType = suppliedValue.providedType ?: return false
				val possibleConversions = parameterType.getConversionsFrom(suppliedType)
				if(possibleConversions.isNotEmpty()) {
					if(possibleConversions.size > 1) {
						context.addIssue(ConversionAmbiguity(source, suppliedType, parameterType, possibleConversions))
						return false
					}
					conversions[suppliedValue] = possibleConversions.first()
					continue
				}
				return false
			}
		}
		return true
	}

	fun compareSpecificity(otherInitializerDefinition: InitializerDefinition): ComparisonResult {
		for(parameterIndex in 0 until max(fixedParameters.size, otherInitializerDefinition.fixedParameters.size)) {
			val parameterType = getParameterTypeAt(parameterIndex) ?: continue
			val otherParameterType = otherInitializerDefinition.getParameterTypeAt(parameterIndex) ?: continue
			if(parameterType != otherParameterType) {
				if(otherParameterType.accepts(parameterType)) return ComparisonResult.HIGHER
				if(parameterType.accepts(otherParameterType)) return ComparisonResult.LOWER
			}
		}
		val otherVariadicParameter = otherInitializerDefinition.variadicParameter
		if(variadicParameter != null && otherVariadicParameter != null) {
			val variadicParameterType = variadicParameter.providedType ?: return ComparisonResult.SAME
			val otherVariadicParameterType = otherVariadicParameter.providedType ?: return ComparisonResult.SAME
			if(variadicParameterType != otherVariadicParameterType) {
				if(otherVariadicParameterType.accepts(variadicParameterType)) return ComparisonResult.HIGHER
				if(otherVariadicParameterType.accepts(variadicParameterType)) return ComparisonResult.LOWER
			}
		}
		return ComparisonResult.SAME
	}

	fun fulfillsInheritanceRequirementsOf(superInitializer: InitializerDefinition): Boolean {
		if(parameters.size != superInitializer.parameters.size)
			return false
		for(parameterIndex in parameters.indices) {
			val superParameterType = superInitializer.parameters[parameterIndex].providedType ?: continue
			val baseParameterType = parameters[parameterIndex].providedType ?: continue
			if(!baseParameterType.accepts(superParameterType))
				return false
		}
		return true
	}

	fun fulfillsInheritanceRequirementsOf(superInitializer: InitializerDefinition, typeSubstitutions: Map<TypeDeclaration, Type>): Boolean {
		if(parameters.size != superInitializer.parameters.size)
			return false
		for(parameterIndex in parameters.indices) {
			val superParameterType =
				superInitializer.parameters[parameterIndex].providedType?.withTypeSubstitutions(typeSubstitutions) ?: continue
			val baseParameterType = parameters[parameterIndex].providedType ?: continue
			if(!baseParameterType.accepts(superParameterType))
				return false
		}
		return true
	}

	fun determineSignatureTypes() {
		parentTypeDeclaration = scope.getSurroundingTypeDeclaration()
			?: throw CompilerError(source, "Initializer expected surrounding type definition.")
		for(localTypeParameter in localTypeParameters)
			localTypeParameter.determineTypes()
		for(parameter in parameters)
			parameter.determineTypes()
		parentTypeDeclaration.scope.addInitializer(this)
	}

	override fun determineTypes() {
		body?.determineTypes()
	}

	override fun analyseDataFlow() {
		analyseDataFlow(initializerTracker)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		if(hasDataFlowBeenAnalysed)
			return
		hasDataFlowBeenAnalysed = true
		val propertiesToBeInitialized = parentTypeDeclaration.scope.getPropertiesToBeInitialized().toMutableList()
		for(member in parentTypeDeclaration.scope.memberDeclarations)
			if(member is PropertyDeclaration)
				initializerTracker.declare(member, member.providedType is StaticType)
		for(parameter in parameters)
			parameter.analyseDataFlow(initializerTracker)
		body?.analyseDataFlow(initializerTracker)
		initializerTracker.calculateEndState()
		initializerTracker.validate()
		propertiesBeingInitialized.addAll(initializerTracker.getPropertiesBeingInitialized())
		propertiesRequiredToBeInitialized.addAll(initializerTracker.getPropertiesRequiredToBeInitialized())
		tracker.addChild("${parentTypeDeclaration.name}.${toString(true)}", initializerTracker)
		propertiesToBeInitialized.removeAll(propertiesBeingInitialized)
		if(propertiesToBeInitialized.isNotEmpty())
			context.addIssue(UninitializedProperties(source, propertiesToBeInitialized))
	}

	override fun validate() {
		super.validate()
		scope.validate()
		validateConvertingKeyword()
		validateOverridingKeyword()
		validateVariadicParameter()
		validateBodyPresent()
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

	private fun validateBodyPresent() {
		if(isAbstract || isNative) {
			if(body != null)
				context.addIssue(ExtraneousBody(source, isAbstract, "initializer", toString()))
		}
	}

	//TODO:
	// - disallow 'native' instances
	// - disallow 'return' in primitive initializer
	// - disallow 'this' in primitive initializer
	// - require initializer call in primitive initializer as last statement of each block

	override fun declare(constructor: LlvmConstructor) {
		if(isAbstract)
			return
		super.declare(constructor)
		val parameterTypes = LinkedList<LlvmType?>()
		parameterTypes.add(Context.EXCEPTION_PARAMETER_INDEX, constructor.pointerType)
		var parameterIndex = Context.THIS_PARAMETER_INDEX
		if(!parentTypeDeclaration.isLlvmPrimitive()) {
			parameterTypes.add(Context.THIS_PARAMETER_INDEX, constructor.pointerType)
			parameterIndex++
		}
		//TODO add local type parameters
		for(valueParameter in parameters) {
			parameterTypes.add(valueParameter.effectiveType?.getLlvmType(constructor))
			valueParameter.index = parameterIndex
			parameterIndex++
		}
		llvmType = if(!isNative && parentTypeDeclaration.isLlvmPrimitive())
			constructor.buildFunctionType(parameterTypes, getPrimitiveLlvmType(constructor, parentTypeDeclaration), isVariadic)
		else
			constructor.buildFunctionType(parameterTypes, constructor.voidType, isVariadic)
		llvmValue = constructor.buildFunction("${parentTypeDeclaration.getFullName()}_Initializer", llvmType)
	}

	private fun getPrimitiveLlvmType(constructor: LlvmConstructor, typeDeclaration: TypeDeclaration): LlvmType {
		if(SpecialType.BOOLEAN.matches(typeDeclaration))
			return constructor.booleanType
		if(SpecialType.BYTE.matches(typeDeclaration))
			return constructor.byteType
		if(SpecialType.INTEGER.matches(typeDeclaration))
			return constructor.i32Type
		if(SpecialType.FLOAT.matches(typeDeclaration))
			return constructor.floatType
		throw CompilerError(source, "Encountered unknown primitive type declaration '${typeDeclaration.name}'.")
	}

	override fun compile(constructor: LlvmConstructor) {
		if(isAbstract)
			return
		val previousBlock = constructor.getCurrentBlock()
		constructor.createAndSelectEntrypointBlock(llvmValue)
		val thisValue = context.getThisParameter(constructor, llvmValue)
		//TODO add local type parameters
		for(valueParameter in parameters) {
			if(valueParameter.isPropertySetter) {
				val propertyAddress = context.resolveMember(constructor, thisValue, valueParameter.name)
				constructor.buildStore(constructor.getParameter(llvmValue, valueParameter.index), propertyAddress)
			}
		}
		if(parentTypeDeclaration.isLlvmPrimitive()) {
			if(isNative)
				constructor.buildReturn()
			else
				super.compile(constructor)
		} else {
			if(isNative)
				context.nativeRegistry.compileNativeImplementation(constructor, this, llvmValue)
			else if(body == null)
				callTrivialSuperInitializers(constructor, thisValue)
			else
				super.compile(constructor)
			if(body?.isInterruptingExecutionBasedOnStructure != true)
				constructor.buildReturn()
		}
		constructor.select(previousBlock)
	}

	private fun callTrivialSuperInitializers(constructor: LlvmConstructor, thisValue: LlvmValue) {
		val exceptionAddress = context.getExceptionParameter(constructor, llvmValue)
		for(superType in parentTypeDeclaration.getDirectSuperTypes()) {
			if(SpecialType.IDENTIFIABLE.matches(superType) || SpecialType.ANY.matches(superType))
				continue
			val superTypeDeclaration = superType.getTypeDeclaration()
			val trivialInitializer =
				(superTypeDeclaration?.staticValueDeclaration?.providedType as? StaticType)?.getInitializer()?.initializer
					?: throw CompilerError(source, "Default initializer in class '${parentTypeDeclaration.name}'" +
						" with super class '${superTypeDeclaration?.name}' without trivial initializer.")
			val parameters = LinkedList<LlvmValue?>()
			parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
			parameters.add(Context.THIS_PARAMETER_INDEX, thisValue)
			constructor.buildFunctionCall(trivialInitializer.llvmType, trivialInitializer.llvmValue, parameters)
			context.continueRaise(constructor, this)
		}
	}

	fun isConvertingFrom(sourceType: Type): Boolean {
		return isConverting && fixedParameters.size == 1 && fixedParameters.first().providedType?.accepts(sourceType) ?: false
	}

	fun getParameterTypeAt(index: Int): Type? {
		return if(index < fixedParameters.size)
			fixedParameters[index].providedType
		else
			(variadicParameter?.providedType as? PluralType)?.baseType
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
