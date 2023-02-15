package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.general.ErrorHandlingContext
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Function
import components.semantic_analysis.semantic_model.values.Operator
import components.syntax_parser.syntax_tree.general.Element
import messages.Message
import java.util.*

class FunctionImplementation(override val source: Element, override val parentDefinition: TypeDefinition?,
							 val scope: BlockScope, genericParameters: List<TypeDefinition>,
							 val parameters: List<Parameter>, val body: ErrorHandlingContext?, returnType: Type?,
							 override val isAbstract: Boolean = false, val isMutating: Boolean = false,
							 val isNative: Boolean = false, val isOverriding: Boolean = false): Unit(source), MemberDeclaration {
	lateinit var parentFunction: Function
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
	val propertiesRequiredToBeInitialized = LinkedList<PropertyDeclaration>()
	val propertiesBeingInitialized = LinkedList<PropertyDeclaration>()

	init {
		scope.unit = this
		addUnits(parameters)
		addUnits(body)
	}

	fun setParent(function: Function) {
		parentFunction = function
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, this.scope)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		if(body == null)
			return
		val functionTracker = VariableTracker()
		super.analyseDataFlow(linter, functionTracker)
		functionTracker.calculateEndState()
		for((declaration, usages) in functionTracker.variables) {
			if(declaration !is PropertyDeclaration)
				continue
			if(usages.first().isRequiredToBeInitialized())
				propertiesRequiredToBeInitialized.add(declaration)
		}
		for((declaration, end) in functionTracker.ends) {
			if(declaration !is PropertyDeclaration)
				continue
			if(end.isPreviouslyInitialized())
				propertiesBeingInitialized.add(declaration)
		}
		tracker.addChild(parentFunction.name, functionTracker)
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
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
						linter.addMessage(source, "Function might complete despite of 'Never' return type.", Message.Type.ERROR)
				} else {
					if(someBlocksCompleteWithoutReturning)
						linter.addMessage(source, "Function might complete without returning a value.", Message.Type.ERROR)

				}
			}
		}
	}

	override fun toString(): String {
		var stringRepresentation = ""
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
