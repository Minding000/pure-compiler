package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.ErrorHandlingContext
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.Element

class FunctionImplementation(override val source: Element, val scope: BlockScope,
							 val genericParameters: List<TypeDefinition>, val parameters: List<Parameter>,
							 body: ErrorHandlingContext?, returnType: Type?, val isNative: Boolean = false,
							 val isOverriding: Boolean = false, val isMutating: Boolean = false): Unit(source) {
	val signature = FunctionSignature(source, genericParameters, parameters.map { parameter -> parameter.type },
		returnType, true)
	var superFunctionImplementation: FunctionImplementation? = null
		set(value) {
			field = value
			signature.superFunctionSignature = value?.signature
		}

	init {
		addUnits(signature, body, returnType)
		addUnits(genericParameters, parameters)
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, this.scope)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
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
