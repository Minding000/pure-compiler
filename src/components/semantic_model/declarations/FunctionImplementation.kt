package components.semantic_model.declarations

import components.code_generation.llvm.models.declarations.FunctionDefinition
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.general.ErrorHandlingContext
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.BlockScope
import components.semantic_model.types.SelfType
import components.semantic_model.types.Type
import components.semantic_model.values.Function
import components.semantic_model.values.Operator
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.constant_conditions.FunctionCompletesDespiteNever
import logger.issues.constant_conditions.FunctionCompletesWithoutReturning
import logger.issues.declaration.ExtraneousBody
import logger.issues.declaration.InvalidVariadicParameterPosition
import logger.issues.declaration.MissingBody
import logger.issues.declaration.MultipleVariadicParameters
import logger.issues.modifiers.*
import util.uppercaseFirstChar
import java.util.*

class FunctionImplementation(override val source: SyntaxTreeNode, override val scope: BlockScope,
							 localTypeParameters: List<GenericTypeDeclaration>, val parameters: List<Parameter>,
							 val body: ErrorHandlingContext?, returnType: Type?,
							 whereClauseConditions: List<WhereClauseCondition> = emptyList(), override val isAbstract: Boolean = false,
							 val isMutating: Boolean = false, val isNative: Boolean = false, val isOverriding: Boolean = false,
							 val isSpecific: Boolean = false, val isMonomorphic: Boolean = false):
	SemanticModel(source, scope), MemberDeclaration, Callable {
	override var parentTypeDeclaration: TypeDeclaration? = null
	lateinit var parentFunction: Function
	val memberType: String
		get() = parentFunction.memberType
	override val memberIdentifier: String
		get() {
			val parentFunction = parentFunction
			return if(parentFunction is Operator)
				signature.getIdentifier(parentFunction.kind)
			else
				signature.getIdentifier(parentFunction.name)
		}
	val isVariadic = parameters.lastOrNull()?.isVariadic ?: false
	val signature = FunctionSignature(source, scope, localTypeParameters, parameters.map { parameter -> parameter.providedType },
		returnType, whereClauseConditions, this)
	var mightReturnValue = false
	var usesOwnTypeAsSelf = false
	private val functionTracker = VariableTracker(context)
	override var hasDataFlowBeenAnalysed = body == null
	override val propertiesRequiredToBeInitialized = LinkedList<PropertyDeclaration>()
	override val propertiesBeingInitialized = LinkedList<PropertyDeclaration>()
	lateinit var unit: FunctionDefinition

	init {
		scope.semanticModel = this
		addSemanticModels(parameters)
		addSemanticModels(body)
	}

	fun setParent(function: Function) {
		parentFunction = function
	}

	override fun determineTypes() {
		super.determineTypes()
		parentTypeDeclaration = scope.getSurroundingTypeDeclaration()
		signature.parentTypeDeclaration = parentTypeDeclaration
	}

	fun fulfillsInheritanceRequirementsOf(superImplementation: FunctionImplementation,
										  typeSubstitutions: Map<TypeDeclaration, Type>): Boolean {
		if(parentFunction.name != superImplementation.parentFunction.name)
			return false
		return signature.fulfillsInheritanceRequirementsOf(superImplementation.signature.withTypeSubstitutions(typeSubstitutions))
	}

	override fun analyseDataFlow() {
		analyseDataFlow(functionTracker)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		if(hasDataFlowBeenAnalysed)
			return
		hasDataFlowBeenAnalysed = true
		super.analyseDataFlow(functionTracker)
		functionTracker.calculateEndState()
		functionTracker.validate()
		propertiesBeingInitialized.addAll(functionTracker.getPropertiesBeingInitialized())
		propertiesRequiredToBeInitialized.addAll(functionTracker.getPropertiesRequiredToBeInitialized())
		tracker.addChild(toString(), functionTracker)
	}

	override fun validate() {
		super.validate()
		scope.validate()
		validateOverridingKeyword()
		validateSpecificKeyword()
		validateMonomorphicKeyword()
		validateParameters()
		validateReturnType()
		validateBodyPresent()
	}

	private fun validateOverridingKeyword() {
		if(signature.superFunctionSignature != null) {
			if(!isOverriding)
				context.addIssue(MissingOverridingKeyword(source, memberType.uppercaseFirstChar(), toString()))
		} else {
			if(isOverriding)
				context.addIssue(OverriddenSuperMissing(source, memberType))
		}
	}

	private fun validateSpecificKeyword() {
		if(usesOwnTypeAsSelf) {
			if(!isSpecific)
				context.addIssue(MissingSpecificKeyword(source, memberType.uppercaseFirstChar(), toString()))
		} else {
			if(isSpecific)
				context.addIssue(ExtraneousSpecificModifier(source, memberType))
		}
	}

	private fun validateMonomorphicKeyword() {
		val hasSelfTypeParameter = signature.parameterTypes.any { parameterType -> parameterType is SelfType }
		if(hasSelfTypeParameter) {
			if(!isMonomorphic)
				context.addIssue(MissingMonomorphicKeyword(source, memberType.uppercaseFirstChar(), toString()))
		} else {
			if(isMonomorphic)
				context.addIssue(ExtraneousMonomorphicModifier(source, memberType))
		}
	}

	private fun validateParameters() {
		for(parameterIndex in 0 until parameters.size - 1) {
			val parameter = parameters[parameterIndex]
			if(parameter.isVariadic) {
				if(isVariadic)
					context.addIssue(MultipleVariadicParameters(source))
				else
					context.addIssue(InvalidVariadicParameterPosition(parameter.source))
			}
		}
	}

	private fun validateReturnType() {
		if(SpecialType.NOTHING.matches(signature.returnType))
			return
		if(body == null)
			return
		var someBlocksCompleteWithoutReturning = false
		var mainBlockCompletesWithoutReturning = true
		for(statement in body.mainBlock.statements) {
			if(statement.isInterruptingExecutionBasedOnStructure) {
				mainBlockCompletesWithoutReturning = false
				break
			}
		}
		if(mainBlockCompletesWithoutReturning) {
			someBlocksCompleteWithoutReturning = true
		} else {
			for(handleBlock in body.handleBlocks) {
				var handleBlockCompletesWithoutReturning = true
				for(statement in handleBlock.block.statements) {
					if(statement.isInterruptingExecutionBasedOnStructure) {
						handleBlockCompletesWithoutReturning = false
						break
					}
				}
				if(handleBlockCompletesWithoutReturning) {
					someBlocksCompleteWithoutReturning = true
					break
				}
			}
		}
		if(SpecialType.NEVER.matches(signature.returnType)) {
			if(someBlocksCompleteWithoutReturning || mightReturnValue)
				context.addIssue(FunctionCompletesDespiteNever(source, memberType))
		} else {
			if(someBlocksCompleteWithoutReturning)
				context.addIssue(FunctionCompletesWithoutReturning(source, memberType))
		}
	}

	private fun validateBodyPresent() {
		if(isAbstract || isNative) {
			if(body != null)
				context.addIssue(ExtraneousBody(source, isAbstract, memberType, toString()))
		} else {
			if(body == null)
				context.addIssue(MissingBody(source, memberType, toString()))
		}
	}

	override fun toUnit(): FunctionDefinition {
		val unit = FunctionDefinition(this, parameters.map(Parameter::toUnit), body?.toUnit())
		this.unit = unit
		return unit
	}

	override fun toString(): String {
		var stringRepresentation = ""
		val parentDefinition = parentTypeDeclaration
		if(parentDefinition != null) {
			stringRepresentation += parentDefinition.name
			if(parentFunction !is Operator)
				stringRepresentation += "."
		}
		val parentFunction = parentFunction
		stringRepresentation += if(parentFunction is Operator)
			signature.toString(false, parentFunction.kind)
		else
			"${parentFunction.name}${signature.toString(false)}"
		return stringRepresentation
	}
}
