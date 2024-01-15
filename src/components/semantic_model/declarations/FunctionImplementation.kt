package components.semantic_model.declarations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
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
import java.util.*

class FunctionImplementation(override val source: SyntaxTreeNode, override val scope: BlockScope,
							 localTypeParameters: List<GenericTypeDeclaration>, val parameters: List<Parameter>,
							 val body: ErrorHandlingContext?, returnType: Type?,
							 whereClauseConditions: List<WhereClauseCondition> = emptyList(), override val isAbstract: Boolean = false,
							 val isMutating: Boolean = false, val isNative: Boolean = false, val isOverriding: Boolean = false,
							 val isSpecific: Boolean = false, val isMonomorphic: Boolean = false):
	SemanticModel(source, scope), MemberDeclaration, Callable {
	override var parentTypeDeclaration: TypeDeclaration? = null
	private lateinit var parentFunction: Function
	override val memberIdentifier: String
		get() {
			val parentFunction = parentFunction
			return if(parentFunction is Operator)
				signature.toString(false, parentFunction.kind)
			else
				"${parentFunction.name}${signature.toString(false)}"
		}
	val isVariadic = parameters.lastOrNull()?.isVariadic ?: false
	val signature = FunctionSignature(source, scope, localTypeParameters, parameters.map { parameter -> parameter.type }, returnType,
		whereClauseConditions, this)
	var mightReturnValue = false
	var usesOwnTypeAsSelf = false
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
		parentTypeDeclaration = scope.getSurroundingTypeDeclaration()
		signature.parentDefinition = parentTypeDeclaration
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
		val parentDefinition = parentTypeDeclaration
		if(parentDefinition != null)
			trackerName += "${parentDefinition.name}."
		trackerName += memberIdentifier
		tracker.addChild(trackerName, functionTracker)
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
				context.addIssue(MissingOverridingKeyword(source, parentFunction.memberType.replaceFirstChar { it.titlecase() },
					toString()))
		} else {
			if(isOverriding)
				context.addIssue(OverriddenSuperMissing(source, parentFunction.memberType))
		}
	}

	private fun validateSpecificKeyword() {
		if(usesOwnTypeAsSelf) {
			if(!isSpecific)
				context.addIssue(MissingSpecificKeyword(source, parentFunction.memberType.replaceFirstChar { it.titlecase() },
					toString()))
		} else {
			if(isSpecific)
				context.addIssue(ExtraneousSpecificModifier(source, parentFunction.memberType))
		}
	}

	private fun validateMonomorphicKeyword() {
		val hasSelfTypeParameter = signature.parameterTypes.any { parameterType -> parameterType is SelfType }
		if(hasSelfTypeParameter) {
			if(!isMonomorphic)
				context.addIssue(MissingMonomorphicKeyword(source, parentFunction.memberType.replaceFirstChar { it.titlecase() },
					toString()))
		} else {
			if(isMonomorphic)
				context.addIssue(ExtraneousMonomorphicModifier(source, parentFunction.memberType))
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
				context.addIssue(FunctionCompletesDespiteNever(source, parentFunction.memberType))
		} else {
			if(someBlocksCompleteWithoutReturning)
				context.addIssue(FunctionCompletesWithoutReturning(source, parentFunction.memberType))
		}
	}

	private fun validateBodyPresent() {
		if(isAbstract || isNative) {
			if(body != null)
				context.addIssue(ExtraneousBody(source, isAbstract, parentFunction.memberType, toString()))
		} else {
			if(body == null)
				context.addIssue(MissingBody(source, parentFunction.memberType, toString()))
		}
	}

	override fun declare(constructor: LlvmConstructor) {
		if(isAbstract)
			return
		super.declare(constructor)
		//TODO add local type parameters
		for(index in parameters.indices)
			parameters[index].index = index + Context.VALUE_PARAMETER_OFFSET
		llvmValue = constructor.buildFunction(memberIdentifier, signature.getLlvmType(constructor))
	}

	override fun compile(constructor: LlvmConstructor) {
		if(isAbstract)
			return
		val previousBlock = constructor.getCurrentBlock()
		if(isNative) {
			context.compileNativeImplementation(constructor, toString(), llvmValue)
			constructor.select(previousBlock)
			return
		}
		constructor.createAndSelectEntrypointBlock(llvmValue)
		super.compile(constructor)
		if(body?.isInterruptingExecutionBasedOnStructure != true)
			constructor.buildReturn()
		constructor.select(previousBlock)

	}

	//TODO add debug info
	@Suppress("unused")
	private fun addDebugInfo(constructor: LlvmConstructor) {
		val file = constructor.debug.createFile("test.pure", ".")
		val parent = file
		val parameterMetadata = parameters.map { parameter -> parameter.type?.getLlvmMetadata(constructor) }
		val typeMetadata = constructor.debug.createFunctionType(file, parameterMetadata)
		val metadata = constructor.debug.createFunction(parent, toString(), file, typeMetadata)
		constructor.debug.attach(metadata, llvmValue)
	}

	override fun toString(): String {
		var stringRepresentation = ""
		val parentDefinition = parentTypeDeclaration
		if(parentDefinition != null) {
			stringRepresentation += parentDefinition.name
			if(parentFunction !is Operator)
				stringRepresentation += "."
		}
		stringRepresentation += memberIdentifier
		return stringRepresentation
	}
}
