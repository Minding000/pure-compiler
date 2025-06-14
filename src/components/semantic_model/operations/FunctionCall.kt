package components.semantic_model.operations

import components.code_generation.llvm.models.operations.FunctionCall
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.*
import components.semantic_model.scopes.Scope
import components.semantic_model.types.*
import components.semantic_model.values.*
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
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
		get() = targetSignature?.hasGenericType ?: false
	val isPrimaryCall: Boolean
		get() = function !is InitializerReference && (function as? MemberAccess)?.member !is InitializerReference
	override val isInterruptingExecutionBasedOnStructure
		get() = targetInitializer?.parentTypeDeclaration?.isLlvmPrimitive() == true && !isPrimaryCall
	override val isInterruptingExecutionBasedOnStaticEvaluation
		get() = isInterruptingExecutionBasedOnStructure

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
			if(typeDeclaration.isAbstract && isPrimaryCall)
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
		return (function as? MemberAccess)?.target?.effectiveType
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
		for(propertyRequiredToBeInitialized in targetImplementation.getPropertiesRequiredToBeInitialized()) {
			val usage = tracker.add(VariableUsage.Kind.READ, propertyRequiredToBeInitialized, this)
			if(!usage.isPreviouslyInitialized())
				requiredButUninitializedProperties.add(propertyRequiredToBeInitialized)
		}
		for(propertyBeingInitialized in targetImplementation.getPropertiesBeingInitialized())
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

	override fun toUnit() = FunctionCall(this, function.toUnit(), valueParameters.map(Value::toUnit))

	fun getSignature(includeParentType: Boolean = true): String {
		var signature = ""
		signature += when(function) {
			is InitializerReference -> function.providedType
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
