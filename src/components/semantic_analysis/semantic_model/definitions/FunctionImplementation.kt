package components.semantic_analysis.semantic_model.definitions

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.general.ErrorHandlingContext
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Function
import components.semantic_analysis.semantic_model.values.Operator
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.constant_conditions.FunctionCompletesDespiteNever
import logger.issues.constant_conditions.FunctionCompletesWithoutReturning
import logger.issues.modifiers.MissingOverridingKeyword
import logger.issues.modifiers.OverriddenSuperMissing
import java.util.*

class FunctionImplementation(override val source: SyntaxTreeNode, override val scope: BlockScope, genericParameters: List<TypeDefinition>,
							 val parameters: List<Parameter>, val body: ErrorHandlingContext?, returnType: Type?,
							 override val isAbstract: Boolean = false, val isMutating: Boolean = false, val isNative: Boolean = false,
							 val isOverriding: Boolean = false): SemanticModel(source, scope), MemberDeclaration, Callable {
	override var parentDefinition: TypeDefinition? = null
	private lateinit var parentFunction: Function
	override val memberIdentifier: String
		get() {
			val parentOperator = parentFunction as? Operator
			return if(parentOperator == null) {
				"${parentFunction.name}${signature.toString(false)}"
			} else {
				signature.toString(false, parentOperator.kind)
			}
		}
	val signature = FunctionSignature(source, scope, genericParameters, parameters.map { parameter -> parameter.type }, returnType)
	var mightReturnValue = false
	override val propertiesRequiredToBeInitialized = LinkedList<PropertyDeclaration>()
	override val propertiesBeingInitialized = LinkedList<PropertyDeclaration>()
	lateinit var llvmValue: LlvmValue

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
		parentDefinition = scope.getSurroundingDefinition()
		signature.parentDefinition = parentDefinition
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		if(body == null)
			return
		val functionTracker = VariableTracker(context)
		super.analyseDataFlow(functionTracker)
		functionTracker.calculateEndState()
		functionTracker.validate()
		propertiesBeingInitialized.addAll(functionTracker.getPropertiesBeingInitialized())
		propertiesRequiredToBeInitialized.addAll(functionTracker.getPropertiesRequiredToBeInitialized())
		var trackerName = ""
		val parentDefinition = parentDefinition
		if(parentDefinition != null)
			trackerName += "${parentDefinition.name}."
		trackerName += memberIdentifier
		tracker.addChild(trackerName, functionTracker)
	}

	override fun validate() {
		super.validate()
		validateOverridingKeyword()
		validateReturnType()
	}

	private fun validateOverridingKeyword() {
		if(signature.superFunctionSignature != null) {
			if(!isOverriding)
				context.addIssue(MissingOverridingKeyword(source, parentFunction.memberType.replaceFirstChar { it.titlecase() },
					toString()))
		} else {
			if(isOverriding)
				context.addIssue(OverriddenSuperMissing(source, parentFunction.memberType))
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
			if(statement.isInterruptingExecution) {
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
					if(statement.isInterruptingExecution) {
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
				context.addIssue(FunctionCompletesDespiteNever(source))
		} else {
			if(someBlocksCompleteWithoutReturning)
				context.addIssue(FunctionCompletesWithoutReturning(source))
		}
	}

	override fun declare(constructor: LlvmConstructor) {
		super.declare(constructor)
		for(index in parameters.indices)
			parameters[index].index = index + 1
		llvmValue = constructor.buildFunction(memberIdentifier, signature.getLlvmType(constructor))
	}

	override fun compile(constructor: LlvmConstructor) {
		val previousBlock = constructor.getCurrentBlock()
		constructor.createAndSelectBlock(llvmValue, "entrypoint")
		super.compile(constructor)
		if(body?.isInterruptingExecution != true)
			constructor.buildReturn()
		constructor.select(previousBlock)
	}

	override fun toString(): String {
		var stringRepresentation = ""
		val parentDefinition = parentDefinition
		if(parentDefinition != null) {
			stringRepresentation += parentDefinition.name
			if(parentFunction !is Operator)
				stringRepresentation += "."
		}
		stringRepresentation += memberIdentifier
		return stringRepresentation
	}
}
