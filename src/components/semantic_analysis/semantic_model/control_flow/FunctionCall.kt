package components.semantic_analysis.semantic_model.control_flow

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.context.Context
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.context.VariableUsage
import components.semantic_analysis.semantic_model.definitions.*
import components.semantic_analysis.semantic_model.operations.MemberAccess
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.FunctionType
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.*
import components.semantic_analysis.semantic_model.values.Function
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.initialization.ReliesOnUninitializedProperties
import logger.issues.modifiers.AbstractClassInstantiation
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

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		staticValue = this
		var targetImplementation: Callable? = targetInitializer
		val targetSignature = targetSignature
		if(targetSignature != null)
			targetImplementation = ((((function as? MemberAccess)?.member ?: function) as? VariableValue)?.definition?.value as? Function)
				?.getImplementationBySignature(targetSignature)
		if(targetImplementation == null)
			return
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

	private fun resolveInitializerCall(targetType: StaticType) {
		(targetType.definition as? Class)?.let { `class` ->
			if(`class`.isAbstract)
				context.addIssue(AbstractClassInstantiation(source, `class`))
		}
		val baseDefinition = targetType.getBaseDefinition()
		val genericDefinitionTypes = baseDefinition.scope.getGenericTypeDefinitions()
		val definitionTypeParameters = (function as? TypeSpecification)?.typeParameters ?: emptyList()
		try {
			val match = targetType.resolveInitializer(genericDefinitionTypes, definitionTypeParameters, typeParameters, valueParameters)
			if(match == null) {
				context.addIssue(NotFound(source, "Initializer", getSignature()))
				return
			}
			val type = ObjectType(match.definitionTypeSubstitutions.map { typeSubstitution -> typeSubstitution.value }, baseDefinition)
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
			targetSignature = functionType.resolveSignature(typeParameters, valueParameters)
			if(targetSignature == null) {
				context.addIssue(SignatureMismatch(function, typeParameters, valueParameters))
				return
			}
			type = targetSignature?.returnType
		} catch(error: SignatureResolutionAmbiguityError) {
			error.log(source, "function", getSignature())
		}
	}

	override fun compile(constructor: LlvmConstructor) {
		createLlvmValue(constructor)
	}

	override fun createLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val parameters = LinkedList<LlvmValue>()
		for(valueParameter in valueParameters)
			parameters.add(valueParameter.getLlvmValue(constructor))
		val functionSignature = targetSignature
		if(functionSignature != null)
			return createLlvmFunctionCall(constructor, functionSignature, parameters)
		val initializerDefinition = targetInitializer ?: throw CompilerError(source, "Function call is missing a target.")
		return createLlvmInitializerCall(constructor, initializerDefinition, parameters)
	}

	private fun createLlvmFunctionCall(constructor: LlvmConstructor, signature: FunctionSignature,
									   parameters: LinkedList<LlvmValue>): LlvmValue {
		if(signature.isVariadic) {
			val fixedParameterCount = signature.fixedParameterTypes.size
			val variadicParameterCount = parameters.size - fixedParameterCount
			parameters.add(fixedParameterCount, constructor.buildInt32(variadicParameterCount))
		}
		val typeDefinition = signature.parentDefinition
		val functionAddress = if(typeDefinition == null) {
			val implementation = ((function as? VariableValue)?.definition?.value as? Function)?.getImplementationBySignature(signature)
				?: throw CompilerError(source, "Failed to determine address of global function.")
			implementation.llvmValue
		} else {
			val targetValue = if(function is MemberAccess)
				function.target.getLlvmValue(constructor)
			else
				context.getThisParameter(constructor)
			parameters.addFirst(targetValue)
			if(function is MemberAccess && function.target is SuperReference) {
				val implementation = ((function.member as? VariableValue)?.definition?.value as? Function)
					?.getImplementationBySignature(signature)
					?: throw CompilerError(source, "Failed to determine address of super function.")
				implementation.llvmValue
			} else {
				val classDefinitionAddressLocation = constructor.buildGetPropertyPointer(typeDefinition.llvmType, targetValue,
					Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinition")
				val classDefinitionAddress = constructor.buildLoad(constructor.createPointerType(context.classDefinitionStruct),
					classDefinitionAddressLocation, "classDefinitionAddress")
				val functionName = (((function as? MemberAccess)?.member ?: function) as? VariableValue)?.name
					?: throw CompilerError(source, "Failed to determine name of member function.")
				val id = context.memberIdentities.getId("${functionName}${signature.toString(false)}")
				constructor.buildFunctionCall(context.llvmFunctionAddressFunctionType, context.llvmFunctionAddressFunction,
					listOf(classDefinitionAddress, constructor.buildInt32(id)), "functionAddress")
			}
		}
		val resultName = if(SpecialType.NOTHING.matches(signature.returnType)) "" else getSignature()
		return constructor.buildFunctionCall(signature.getLlvmType(constructor), functionAddress, parameters, resultName)
	}

	private fun createLlvmInitializerCall(constructor: LlvmConstructor, initializer: InitializerDefinition,
										  parameters: LinkedList<LlvmValue>): LlvmValue {
		//TODO primary initializer calls should also be resolved dynamically (for generic type initialization)
		// - unless the variable definition is a specific type already (or can be traced to it)
		val isPrimaryCall = function !is InitializerReference && (function as? MemberAccess)?.member !is InitializerReference
		return if(isPrimaryCall) {
			val typeDefinition = initializer.parentDefinition
			val newObjectAddress = constructor.buildHeapAllocation(typeDefinition.llvmType, "newObjectAddress")
			val classDefinitionPointer = constructor.buildGetPropertyPointer(typeDefinition.llvmType, newObjectAddress,
				Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinitionPointer")
			constructor.buildStore(typeDefinition.llvmClassDefinitionAddress, classDefinitionPointer)
			parameters.addFirst(newObjectAddress)
			constructor.buildFunctionCall(initializer.llvmType, initializer.llvmValue, parameters)
			newObjectAddress
		} else {
			parameters.addFirst(context.getThisParameter(constructor))
			constructor.buildFunctionCall(initializer.llvmType, initializer.llvmValue, parameters)
		}
	}

	private fun getSignature(): String {
		var signature = ""
		signature += when(function) {
			is VariableValue -> function.name
			is TypeSpecification -> function
			is MemberAccess -> "${function.target.type}.${function.member}"
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
