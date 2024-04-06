package components.semantic_model.operations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.code_generation.llvm.ValueConverter
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.*
import components.semantic_model.scopes.Scope
import components.semantic_model.types.*
import components.semantic_model.values.*
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.access.AbstractMonomorphicAccess
import logger.issues.access.WhereClauseUnfulfilled
import logger.issues.initialization.ReliesOnUninitializedProperties
import logger.issues.modifiers.AbstractClassInstantiation
import logger.issues.resolution.CallToSpecificSuperMember
import logger.issues.resolution.NotCallable
import logger.issues.resolution.NotFound
import logger.issues.resolution.SignatureMismatch
import java.util.*

class FunctionCall(override val source: SyntaxTreeNode, scope: Scope, val function: Value, val typeParameters: List<Type> = emptyList(),
				   val valueParameters: List<Value> = emptyList()): Value(source, scope) {
	var targetInitializer: InitializerDefinition? = null
	var targetSignature: FunctionSignature? = null
	var conversions: Map<Value, InitializerDefinition>? = null
	val globalTypeParameters = LinkedList<Type>()
	override val hasGenericType: Boolean
		get() = targetSignature?.original?.returnType != targetSignature?.returnType

	init {
		addSemanticModels(typeParameters, valueParameters)
		addSemanticModels(function)
	}

	override fun determineTypes() {
		super.determineTypes()
		when(val targetType = function.effectiveType) {
			is StaticType -> resolveInitializerCall(targetType)
			is FunctionType -> resolveFunctionCall(targetType)
			null -> {}
			else -> context.addIssue(NotCallable(function))
		}
	}

	private fun resolveInitializerCall(targetType: StaticType) {
		val typeDeclaration = targetType.typeDeclaration
		if(typeDeclaration is Class) {
			if(typeDeclaration.isAbstract)
				context.addIssue(AbstractClassInstantiation(source, typeDeclaration))
		}
		val globalTypeParameters = typeDeclaration.scope.getGenericTypeDeclarations()
		val suppliedGlobalTypes = (function as? TypeSpecification)?.globalTypes ?: emptyList()
		try {
			val match = targetType.getInitializer(globalTypeParameters, suppliedGlobalTypes, typeParameters, valueParameters)
			if(match == null) {
				context.addIssue(NotFound(source, "Initializer", getSignature()))
				return
			}
			val type = ObjectType(match.globalTypeSubstitutions.values.toList(), targetType.typeDeclaration)
			addSemanticModels(type)
			providedType = type
			targetInitializer = match.initializer
			conversions = match.conversions
			for((_, typeParameter) in match.globalTypeSubstitutions)
				this.globalTypeParameters.add(typeParameter)
		} catch(error: SignatureResolutionAmbiguityError) {
			error.log(source, "initializer", getSignature())
		}
	}

	private fun resolveFunctionCall(functionType: FunctionType) {
		try {
			val match = functionType.getSignature(typeParameters, valueParameters)
			if(match == null) {
				context.addIssue(SignatureMismatch(function, typeParameters, valueParameters))
				return
			}
			targetSignature = match.signature
			conversions = match.conversions
			val targetType = getTargetType()
			var returnType: Type? = match.returnType
			if(targetType != null)
				returnType = returnType?.getLocalType(this, targetType)
			setUnextendedType(returnType)
			registerSelfTypeUsages(match.signature)
		} catch(error: SignatureResolutionAmbiguityError) {
			error.log(source, "function", getSignature())
		}
	}

	private fun getTargetType(): Type? {
		//TODO improve 'targetType' determination
		return (function as? MemberAccess)?.target?.providedType
	}

	//TODO do the same for initializer calls
	//TODO do the same for all operator calls
	private fun registerSelfTypeUsages(signature: FunctionSignature) {
		for((index, parameter) in valueParameters.withIndex()) {
			val sourceType = parameter.providedType
			val baseSourceType = if(sourceType is OptionalType) sourceType.baseType else sourceType
			if(baseSourceType !is SelfType) {
				val surroundingFunction = scope.getSurroundingFunction()
				val targetType = signature.getParameterTypeAt(index)
				val baseTargetType = if(targetType is OptionalType) targetType.baseType else targetType
				if(baseTargetType is SelfType)
					surroundingFunction?.usesOwnTypeAsSelf = true
			}
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		staticValue = this
		if(function is MemberAccess && function.target !is SelfReference && function.target !is SuperReference)
			return
		if(function.effectiveType is StaticType) {
			if(function is MemberAccess) {
				if(function.member !is InitializerReference)
					return
			} else {
				if(function !is InitializerReference)
					return
			}
		}
		val targetImplementation = targetInitializer ?: targetSignature?.associatedImplementation ?: return
		//TODO also track required and initialized properties for operators (IndexAccess, BinaryOperator, etc.)
		val requiredButUninitializedProperties = LinkedList<PropertyDeclaration>()
		for(propertyRequiredToBeInitialized in targetImplementation.propertiesRequiredToBeInitialized) {
			val usage = tracker.add(VariableUsage.Kind.READ, propertyRequiredToBeInitialized, this)
			if(!usage.isPreviouslyInitialized())
				requiredButUninitializedProperties.add(propertyRequiredToBeInitialized)
		}
		for(propertyBeingInitialized in targetImplementation.propertiesBeingInitialized)
			tracker.add(VariableUsage.Kind.WRITE, propertyBeingInitialized, this)
		if(tracker.isInitializer && requiredButUninitializedProperties.isNotEmpty())
			context.addIssue(ReliesOnUninitializedProperties(source, getSignature(), requiredButUninitializedProperties))
	}

	override fun validate() {
		super.validate()
		validateCallToSpecificFunction()
		validateWhereClauseConditions()
		validateMonomorphicAccess()
	}

	private fun validateCallToSpecificFunction() {
		if(function is MemberAccess && function.target is SuperReference && targetSignature?.associatedImplementation?.isSpecific == true)
			context.addIssue(CallToSpecificSuperMember(source))
	}

	private fun validateWhereClauseConditions() {
		val signature = targetSignature ?: return
		val targetType = getTargetType() ?: return
		val typeParameters = (targetType as? ObjectType)?.typeParameters ?: emptyList()
		for(condition in signature.whereClauseConditions) {
			if(!condition.isMet(typeParameters))
				context.addIssue(WhereClauseUnfulfilled(source, "Function", getSignature(false), targetType,
					condition))
		}
	}

	private fun validateMonomorphicAccess() {
		val signature = targetSignature ?: return
		val targetType = getTargetType() ?: return
		if(signature.associatedImplementation?.isAbstract == true && signature.associatedImplementation.isMonomorphic
			&& !targetType.isMemberAccessible(signature, true))
			context.addIssue(AbstractMonomorphicAccess(source, "function",
				signature.toString(false), targetType))
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val exceptionAddress = context.getExceptionParameter(constructor)
		val functionSignature = targetSignature
		val returnValue = if(functionSignature == null) {
			val initializerDefinition = targetInitializer ?: throw CompilerError(source, "Function call is missing a target.")
			buildLlvmInitializerCall(constructor, initializerDefinition, exceptionAddress)
		} else {
			buildLlvmFunctionCall(constructor, functionSignature, exceptionAddress)
		}
		context.continueRaise(constructor)
		return returnValue
	}

	private fun buildLlvmFunctionCall(constructor: LlvmConstructor, signature: FunctionSignature, exceptionAddress: LlvmValue): LlvmValue {
		val parameters = LinkedList<LlvmValue>()
		//TODO add local type parameters
		for((index, valueParameter) in valueParameters.withIndex()) {
			if(valueParameter is UnaryOperator && valueParameter.kind == Operator.Kind.TRIPLE_DOT) {
				TODO("The spread operator is not implemented yet.")
				//TODO scrap va_lists, because they require a static parameter count
			} else {
				val parameterType = targetSignature?.getParameterTypeAt(index)
				parameters.add(ValueConverter.convertIfRequired(this, constructor, valueParameter.getLlvmValue(constructor),
					valueParameter.providedType, valueParameter.hasGenericType, parameterType,
					parameterType != targetSignature?.original?.getParameterTypeAt(index), conversions?.get(valueParameter)))
			}
		}
		if(signature.isVariadic) {
			val fixedParameterCount = signature.fixedParameterTypes.size
			val variadicParameterCount = parameters.size - fixedParameterCount
			parameters.add(fixedParameterCount, constructor.buildInt32(variadicParameterCount))
		}
		val typeDefinition = signature.parentTypeDeclaration
		val functionAddress = if(typeDefinition == null) {
			val implementation = signature.associatedImplementation
			if(implementation == null) {
				//TODO add captured variables as parameters
				val closureLocation = function.getLlvmValue(constructor)
				constructor.buildGetPropertyPointer(context.closureStruct, closureLocation, Context.CLOSURE_FUNCTION_ADDRESS_PROPERTY_INDEX,
					"_functionAddress")
			} else {
				implementation.llvmValue
			}
		} else if(typeDefinition.isLlvmPrimitive()) {
			//TODO same for operators and getters
			//TODO does this work for optional primitives?
			val targetValue = if(function is MemberAccess)
				function.target.getLlvmValue(constructor)
			else
				context.getThisParameter(constructor)
			val functionName = (((function as? MemberAccess)?.member ?: function) as? VariableValue)?.name
				?: throw CompilerError(source, "Failed to determine name of member function.")
			val primitiveImplementation = context.nativeRegistry.resolvePrimitiveImplementation(
				"${typeDefinition.name}.${functionName}${signature.original.toString(false)}")
			parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
			parameters.add(Context.THIS_PARAMETER_INDEX, targetValue)
			val resultName = if(SpecialType.NOTHING.matches(signature.returnType)) "" else getSignature()
			return constructor.buildFunctionCall(primitiveImplementation.llvmType, primitiveImplementation.llvmValue, parameters,
				resultName)
		} else {
			val targetValue = if(function is MemberAccess)
				function.target.getLlvmValue(constructor)
			else
				context.getThisParameter(constructor)
			parameters.addFirst(targetValue)
			if(function is MemberAccess && function.target is SuperReference) {
				val implementation = signature.associatedImplementation
					?: throw CompilerError(source, "Encountered member signature without implementation.")
				implementation.llvmValue
			} else {
				val functionName = (((function as? MemberAccess)?.member ?: function) as? VariableValue)?.name
					?: throw CompilerError(source, "Failed to determine name of member function.")
				context.resolveFunction(constructor, targetValue,
					"${functionName}${signature.original.toString(false)}")
			}
		}
		parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
		val resultName = if(SpecialType.NOTHING.matches(signature.returnType)) "" else getSignature()
		return constructor.buildFunctionCall(signature.original.getLlvmType(constructor), functionAddress, parameters, resultName)
	}

	private fun buildLlvmInitializerCall(constructor: LlvmConstructor, initializer: InitializerDefinition,
										 exceptionAddress: LlvmValue): LlvmValue {
		val isPrimaryCall = function !is InitializerReference && (function as? MemberAccess)?.member !is InitializerReference
		val parameters = LinkedList<LlvmValue?>()
		//TODO add local type parameters
		for((index, valueParameter) in valueParameters.withIndex())
			parameters.add(ValueConverter.convertIfRequired(this, constructor, valueParameter.getLlvmValue(constructor),
				valueParameter.providedType, initializer.getParameterTypeAt(index), conversions?.get(valueParameter)))
		if(initializer.isVariadic) {
			val fixedParameterCount = initializer.fixedParameters.size
			val variadicParameterCount = parameters.size - fixedParameterCount
			parameters.add(fixedParameterCount, constructor.buildInt32(variadicParameterCount))
		}
		//TODO primary initializer calls should also be resolved dynamically (for generic type initialization)
		// - unless the variable definition is a specific type already (or can be traced to it)
		return if(isPrimaryCall) {
			if(initializer.parentTypeDeclaration.isLlvmPrimitive()) {
				val signature = initializer.toString()
				if(initializer.isNative)
					return context.nativeRegistry.inlineNativePrimitiveInitializer(constructor, "$signature: Self", parameters)
				parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
				return constructor.buildFunctionCall(initializer.llvmType, initializer.llvmValue, parameters, signature)
			}
			val typeDeclaration = initializer.parentTypeDeclaration
			val newObject = constructor.buildHeapAllocation(typeDeclaration.llvmType, "newObject")
			val classDefinitionProperty = constructor.buildGetPropertyPointer(typeDeclaration.llvmType, newObject,
				Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinitionProperty")
			constructor.buildStore(typeDeclaration.llvmClassDefinition, classDefinitionProperty)
			buildLlvmCommonPreInitializerCall(constructor, initializer.parentTypeDeclaration, exceptionAddress, newObject)
			parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
			parameters.add(Context.THIS_PARAMETER_INDEX, newObject)
			constructor.buildFunctionCall(initializer.llvmType, initializer.llvmValue, parameters)
			newObject
		} else if(initializer.parentTypeDeclaration.isLlvmPrimitive()) {
			if(parameters.size != 1)
				throw CompilerError("Invalid number of arguments passed to '${getSignature()}': ${parameters.size}")
			val firstParameter = parameters.firstOrNull() ?: throw CompilerError("Parameter for '${getSignature()}' is null.")
			constructor.buildReturn(firstParameter)
			constructor.nullPointer
		} else {
			parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
			parameters.add(Context.THIS_PARAMETER_INDEX, context.getThisParameter(constructor))
			constructor.buildFunctionCall(initializer.llvmType, initializer.llvmValue, parameters)
			constructor.nullPointer
		}
	}

	private fun buildLlvmCommonPreInitializerCall(constructor: LlvmConstructor, typeDeclaration: TypeDeclaration,
												  exceptionAddress: LlvmValue, newObject: LlvmValue) {
		val parameters = LinkedList<LlvmValue?>()
		parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
		parameters.add(Context.THIS_PARAMETER_INDEX, newObject)
		if(typeDeclaration.isBound) {
			val parent = (function as? MemberAccess)?.target?.getLlvmValue(constructor) ?: context.getThisParameter(constructor)
			parameters.add(Context.PARENT_PARAMETER_OFFSET, parent)
		}
		//TODO how are complex types passed? e.g. <Int, Byte>Map or Int? or Int | Cat
		// What are they used for?
		// -> type check (could be limited to ObjectType)
		// -> instantiation (only works with ObjectTypes, may include generic types)
		for(typeParameter in globalTypeParameters) {
			val objectType = typeParameter.effectiveType as? ObjectType
				?: throw CompilerError(typeParameter.source, "Only object types are allowed as type parameters.")
			parameters.add(objectType.getStaticLlvmValue(constructor))
		}
		constructor.buildFunctionCall(typeDeclaration.llvmCommonPreInitializerType, typeDeclaration.llvmCommonPreInitializer, parameters)
		context.continueRaise(constructor)
	}

	private fun getSignature(includeParentType: Boolean = true): String {
		var signature = ""
		signature += when(function) {
			is VariableValue -> function.name
			is TypeSpecification -> function
			is MemberAccess -> if(includeParentType) "${function.target.providedType}.${function.member}" else function.member
			else -> "<anonymous function>"
		}
		signature += "("
		if(typeParameters.isNotEmpty()) {
			signature += typeParameters.joinToString()
			signature += ";"
			if(valueParameters.isNotEmpty())
				signature += " "
		}
		signature += valueParameters.joinToString { parameter -> parameter.providedType.toString() }
		signature += ")"
		return signature
	}
}
