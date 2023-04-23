package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.general.ErrorHandlingContext
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Function
import components.semantic_analysis.semantic_model.values.Operator
import components.syntax_parser.syntax_tree.general.Element
import logger.issues.constant_conditions.FunctionCompletesDespiteNever
import logger.issues.constant_conditions.FunctionCompletesWithoutReturning
import logger.issues.modifiers.MissingOverridingKeyword
import logger.issues.modifiers.OverriddenSuperMissing
import java.util.*

class FunctionImplementation(override val source: Element, override val scope: BlockScope, genericParameters: List<TypeDefinition>,
							 val parameters: List<Parameter>, val body: ErrorHandlingContext?, returnType: Type?,
							 override val isAbstract: Boolean = false, val isMutating: Boolean = false, val isNative: Boolean = false,
							 val isOverriding: Boolean = false): Unit(source, scope), MemberDeclaration, Callable {
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
	val signature: FunctionSignature = FunctionSignature(source, scope, genericParameters,
		parameters.map { parameter -> parameter.type }, returnType, true)
	var mightReturnValue = false
	override val propertiesRequiredToBeInitialized = LinkedList<PropertyDeclaration>()
	override val propertiesBeingInitialized = LinkedList<PropertyDeclaration>()

	init {
		scope.unit = this
		addUnits(parameters)
		addUnits(body)
	}

	fun setParent(function: Function) {
		parentFunction = function
	}

	override fun linkTypes(linter: Linter) {
		super.linkTypes(linter)
		parentDefinition = scope.getSurroundingDefinition()
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		if(body == null)
			return
		val functionTracker = VariableTracker(tracker.linter)
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

	override fun validate(linter: Linter) {
		super.validate(linter)
		if(signature.superFunctionSignature != null) {
			if(!isOverriding)
				linter.addIssue(MissingOverridingKeyword(source, parentFunction.memberType.replaceFirstChar { it.titlecase() }, toString()))
		} else {
			if(isOverriding)
				linter.addIssue(OverriddenSuperMissing(source, parentFunction.memberType))
		}
		if(!Linter.SpecialType.NOTHING.matches(signature.returnType)) {
			if(body != null) {
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
				if(Linter.SpecialType.NEVER.matches(signature.returnType)) {
					if(someBlocksCompleteWithoutReturning || mightReturnValue)
						linter.addIssue(FunctionCompletesDespiteNever(source))
				} else {
					if(someBlocksCompleteWithoutReturning)
						linter.addIssue(FunctionCompletesWithoutReturning(source))

				}
			}
		}
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

//	override fun compile(context: BuildContext): LLVMValueRef {
//		val argumentTypes = PointerPointer<Pointer>(parameters.size.toLong())
//		for(parameter in parameters)
//			argumentTypes.put(parameter.compile(context))
//		val returnType = returnType?.compile(context) ?: LLVMVoidType()
//		val functionType = LLVMFunctionType(returnType, argumentTypes, parameters.size, LLVMIRCompiler.LLVM_NO)
//		val function = LLVMAddFunction(context.module, name, functionType)
//		LLVMSetFunctionCallConv(function, LLVMCCallConv)
//		return function
//	}
}
