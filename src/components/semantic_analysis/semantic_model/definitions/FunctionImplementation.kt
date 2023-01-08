package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.ErrorHandlingContext
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Function
import components.semantic_analysis.semantic_model.values.Operator
import components.syntax_parser.syntax_tree.general.Element
import messages.Message

class FunctionImplementation(override val source: Element, override val parentDefinition: TypeDefinition?,
							 val scope: BlockScope, genericParameters: List<TypeDefinition>,
							 val parameters: List<Parameter>, val body: ErrorHandlingContext?, returnType: Type?,
							 override val isAbstract: Boolean = false, val isMutating: Boolean = false,
							 val isNative: Boolean = false, val isOverriding: Boolean = false): Unit(source), MemberDeclaration {
	override lateinit var memberIdentifier: String
	lateinit var parentFunction: Function
	val signature: FunctionSignature = FunctionSignature(source, scope, genericParameters,
		parameters.map { parameter -> parameter.type }, returnType, true)
	var superFunctionImplementation: FunctionImplementation? = null
		set(value) {
			field = value
			signature.superFunctionSignature = value?.signature
		}
	var mightReturnValue = false

	init {
		scope.unit = this
		addUnits(body)
		addUnits(parameters)
	}

	fun setParent(function: Function) {
		parentFunction = function
		memberIdentifier = if(function is Operator) {
			signature.toString(false, function.kind)
		} else {
			"${function.name}${signature.toString(false)}"
		}
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, this.scope)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		if(!Linter.LiteralType.NOTHING.matches(signature.returnType)) {
			if(!mightReturnValue)
				linter.addMessage(source, "Function never returns.", Message.Type.ERROR)
			if(body != null) {
				var someBlocksCompletesWithoutReturning = false
				var mainBlockCompletesWithoutReturning = true
				for(statement in body.mainBlock.statements) {
					if(statement.isInterruptingExecution) {
						mainBlockCompletesWithoutReturning = false
						break
					}
				}
				if(mainBlockCompletesWithoutReturning) {
					someBlocksCompletesWithoutReturning = true
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
							someBlocksCompletesWithoutReturning = true
							break
						}
					}
				}
				if(someBlocksCompletesWithoutReturning)
					linter.addMessage(source, "Function might complete without returning a value.", Message.Type.ERROR)
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
