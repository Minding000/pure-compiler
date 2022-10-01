package linting.semantic_model.definitions

import linting.Linter
import linting.semantic_model.general.ErrorHandlingContext
import linting.semantic_model.general.Unit
import linting.semantic_model.literals.Type
import linting.semantic_model.scopes.BlockScope
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.general.Element

class FunctionImplementation(val source: Element, val scope: BlockScope, val genericParameters: List<TypeDefinition>,
							 val parameters: List<Parameter>, body: ErrorHandlingContext?, val returnType: Type?,
							 val isNative: Boolean = false, val isOverriding: Boolean = false): Unit() {
	val signature = FunctionSignature(source, genericParameters, parameters.map { parameter -> parameter.type },
		returnType)
	var superFunctionImplementation: FunctionImplementation? = null
		set(value) {
			field = value
			signature.superFunctionSignature = value?.signature
		}

	init {
		units.addAll(genericParameters)
		units.addAll(parameters)
		if(body != null)
			units.add(body)
		if(returnType != null)
			units.add(returnType)
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