package components.semantic_model.control_flow

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.*
import components.semantic_model.operations.MemberAccess
import components.semantic_model.scopes.Scope
import components.semantic_model.types.FunctionType
import components.semantic_model.types.ObjectType
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import components.semantic_model.values.InitializerReference
import components.semantic_model.values.SuperReference
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue
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

	private fun resolveInitializerCall(targetType: StaticType) {
		(targetType.typeDeclaration as? Class)?.let { `class` ->
			if(`class`.isAbstract)
				context.addIssue(AbstractClassInstantiation(source, `class`))
		}
		val globalTypeParameters = targetType.typeDeclaration.scope.getGenericTypeDeclarations()
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
			type = match.returnType
		} catch(error: SignatureResolutionAmbiguityError) {
			error.log(source, "function", getSignature())
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
					"${functionName}${signature.toString(false)}")
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
			val typeDefinition = initializer.parentTypeDeclaration
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
