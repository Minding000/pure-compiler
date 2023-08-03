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
import components.semantic_analysis.semantic_model.values.Function
import components.semantic_analysis.semantic_model.values.SuperReference
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
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
	var targetImplementation: MemberDeclaration? = null

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
		val targetImplementation = targetImplementation
		if(targetImplementation !is Callable)
			return
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
			val match = targetType.resolveInitializer(genericDefinitionTypes, definitionTypeParameters, typeParameters,
				valueParameters)
			if(match == null) {
				context.addIssue(NotFound(source, "Initializer", getSignature()))
				return
			}
			val type = ObjectType(match.definitionTypeSubstitutions.map { typeSubstitution -> typeSubstitution.value }, baseDefinition)
			type.determineTypes()
			addSemanticModels(type)
			this.type = type
			targetImplementation = match.signature
		} catch(error: SignatureResolutionAmbiguityError) {
			error.log(source, "initializer", getSignature())
		}
	}

	private fun resolveFunctionCall(functionType: FunctionType) {
		try {
			val signature = functionType.resolveSignature(typeParameters, valueParameters)
			if(signature == null) {
				context.addIssue(SignatureMismatch(function, typeParameters, valueParameters))
				return
			}
			type = signature.returnType
			val variable = ((function as? MemberAccess)?.member ?: function) as? VariableValue
			val property = variable?.definition as? PropertyDeclaration
			val function = property?.value as? Function
			targetImplementation = function?.getImplementationBySignature(signature)
		} catch(error: SignatureResolutionAmbiguityError) {
			error.log(source, "function", getSignature())
		}
	}

	override fun createLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val parameters = LinkedList<LlvmValue>()
		for(valueParameter in valueParameters)
			parameters.add(valueParameter.getLlvmValue(constructor))
		return when(val target = targetImplementation) {
			is FunctionImplementation -> {
				val resultName = if(SpecialType.NOTHING.matches(target.signature.returnType)) "" else getSignature()
				val targetValue = if(function is MemberAccess && function.target !is SuperReference)
					function.target.getLlvmValue(constructor)
				else
					context.getThisParameter(constructor)
				if(target.parentDefinition != null)
					parameters.addFirst(targetValue)
				val functionAddress = if(function is MemberAccess && function.target is SuperReference) {
					target.llvmValue
				} else {
					val classDefinitionAddressLocation = constructor.buildGetPropertyPointer(target.parentDefinition?.llvmType, targetValue,
						Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinition")
					val classDefinitionAddress = constructor.buildLoad(constructor.createPointerType(context.classDefinitionStruct),
						classDefinitionAddressLocation, "classDefinitionAddress")
					constructor.buildFunctionCall(context.llvmFunctionAddressFunctionType,
						context.llvmFunctionAddressFunction,
						listOf(classDefinitionAddress, constructor.buildInt32(context.memberIdentities.getId(target.memberIdentifier))),
						"functionAddress")
				}
				constructor.buildFunctionCall(target.signature.getLlvmType(constructor), functionAddress, parameters, resultName)
			}
			is InitializerDefinition -> {
				constructor.buildFunctionCall(target.llvmType, target.llvmValue, parameters, "newObjectAddress")
			}
			else -> throw CompilerError(source, "Target of type '${target?.javaClass?.simpleName}' is not callable.")
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
