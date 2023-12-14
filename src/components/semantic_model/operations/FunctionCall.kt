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
import components.semantic_model.values.InitializerReference
import components.semantic_model.values.SuperReference
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue
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

	init {
		addSemanticModels(typeParameters, valueParameters)
		addSemanticModels(function)
	}

	override fun determineTypes() {
		super.determineTypes()
		when(val targetType = function.type?.effectiveType) {
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
			type.determineTypes()
			addSemanticModels(type)
			this.type = type
			targetInitializer = match.initializer
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
			val targetType = getTargetType()
			var returnType: Type? = match.returnType
			if(targetType != null)
				returnType = returnType?.getLocalType(this, targetType)
			setUnextendedType(returnType)
			registerSelfTypeUsages(match.signature)
			if(match.signature.associatedImplementation?.isAbstract == true && match.signature.associatedImplementation.isMonomorphic
				&& targetType?.isMemberAccessible(match.signature, true) == false)
				context.addIssue(AbstractMonomorphicAccess(source, "function",
					match.signature.toString(false), targetType))
		} catch(error: SignatureResolutionAmbiguityError) {
			error.log(source, "function", getSignature())
		}
	}

	private fun getTargetType(): Type? {
		//TODO improve 'targetType' determination
		return (function as? MemberAccess)?.target?.type
	}

	//TODO do the same for initializer calls
	//TODO do the same for all operator calls
	private fun registerSelfTypeUsages(signature: FunctionSignature) {
		for((index, parameter) in valueParameters.withIndex()) {
			val sourceType = parameter.type
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

	override fun compile(constructor: LlvmConstructor) {
		createLlvmValue(constructor)
	}

	override fun createLlvmValue(constructor: LlvmConstructor): LlvmValue {

		//TODO reuse pointer provided by caller?
		val exceptionAddressLocation = constructor.buildStackAllocation(constructor.pointerType, "exceptionAddress")

		val functionSignature = targetSignature
		val returnValue = if(functionSignature == null) {
			val initializerDefinition = targetInitializer ?: throw CompilerError(source, "Function call is missing a target.")
			createLlvmInitializerCall(constructor, initializerDefinition, exceptionAddressLocation)
		} else {
			createLlvmFunctionCall(constructor, functionSignature, exceptionAddressLocation)
		}

		//TODO if exception exists
		// check for optional try (normal and force try have no effect)
		// check for catch
		// resume raise

		return returnValue
	}

	private fun createLlvmFunctionCall(constructor: LlvmConstructor, signature: FunctionSignature,
									   exceptionAddressLocation: LlvmValue): LlvmValue {
		val parameters = LinkedList<LlvmValue>()
		for((index, valueParameter) in valueParameters.withIndex())
			parameters.add(ValueConverter.convertIfRequired(this, constructor, valueParameter.getLlvmValue(constructor),
				valueParameter.type, targetSignature?.getParameterTypeAt(index)))
		if(signature.isVariadic) {
			val fixedParameterCount = signature.fixedParameterTypes.size
			val variadicParameterCount = parameters.size - fixedParameterCount
			parameters.add(fixedParameterCount, constructor.buildInt32(variadicParameterCount))
		}
		val typeDefinition = signature.parentDefinition
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
				context.resolveFunction(constructor, typeDefinition.llvmType, targetValue,
					"${functionName}${signature.original.toString(false)}")
			}
		}
		parameters.addFirst(exceptionAddressLocation)
		val resultName = if(SpecialType.NOTHING.matches(signature.returnType)) "" else getSignature()
		return constructor.buildFunctionCall(signature.getLlvmType(constructor), functionAddress, parameters, resultName)
	}

	private fun createLlvmInitializerCall(constructor: LlvmConstructor, initializer: InitializerDefinition,
										  exceptionAddressLocation: LlvmValue): LlvmValue {
		val parameters = LinkedList<LlvmValue?>()
		for((index, valueParameter) in valueParameters.withIndex())
			parameters.add(ValueConverter.convertIfRequired(this, constructor, valueParameter.getLlvmValue(constructor),
				valueParameter.type, targetInitializer?.getParameterTypeAt(index)))
		if(initializer.isVariadic) {
			val fixedParameterCount = initializer.fixedParameters.size
			val variadicParameterCount = parameters.size - fixedParameterCount
			parameters.add(fixedParameterCount, constructor.buildInt32(variadicParameterCount))
		}
		//TODO primary initializer calls should also be resolved dynamically (for generic type initialization)
		// - unless the variable definition is a specific type already (or can be traced to it)
		val isPrimaryCall = function !is InitializerReference && (function as? MemberAccess)?.member !is InitializerReference
		return if(isPrimaryCall) {
			val typeDeclaration = initializer.parentTypeDeclaration
			val newObjectAddress = constructor.buildHeapAllocation(typeDeclaration.llvmType, "newObjectAddress")
			val classDefinitionPointer = constructor.buildGetPropertyPointer(typeDeclaration.llvmType, newObjectAddress,
				Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinitionPointer")
			constructor.buildStore(typeDeclaration.llvmClassDefinitionAddress, classDefinitionPointer)
			parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddressLocation)
			parameters.add(Context.THIS_PARAMETER_INDEX, newObjectAddress)
			if(typeDeclaration.isBound) {
				val parent = (function as? MemberAccess)?.target?.getLlvmValue(constructor) ?: context.getThisParameter(constructor)
				parameters.add(Context.PARENT_PARAMETER_OFFSET, parent)
			}
			constructor.buildFunctionCall(initializer.llvmType, initializer.llvmValue, parameters)
			newObjectAddress
		} else {
			parameters.addFirst(exceptionAddressLocation)
			parameters.add(Context.THIS_PARAMETER_INDEX, context.getThisParameter(constructor))
			constructor.buildFunctionCall(initializer.llvmType, initializer.llvmValue, parameters)
		}
	}

	private fun getSignature(includeParentType: Boolean = true): String {
		var signature = ""
		signature += when(function) {
			is VariableValue -> function.name
			is TypeSpecification -> function
			is MemberAccess -> if(includeParentType) "${function.target.type}.${function.member}" else function.member
			else -> "<anonymous function>"
		}
		signature += "("
		if(typeParameters.isNotEmpty()) {
			signature += typeParameters.joinToString()
			signature += ";"
			if(valueParameters.isNotEmpty())
				signature += " "
		}
		signature += valueParameters.joinToString { parameter -> parameter.type.toString() }
		signature += ")"
		return signature
	}
}
