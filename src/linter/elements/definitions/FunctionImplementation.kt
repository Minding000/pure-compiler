package linter.elements.definitions

import linter.Linter
import linter.elements.general.ErrorHandlingContext
import linter.elements.general.Unit
import linter.elements.literals.Type
import linter.elements.values.TypeDefinition
import linter.scopes.BlockScope
import linter.scopes.Scope
import parsing.ast.general.Element

class FunctionImplementation(val source: Element, val scope: BlockScope, val genericParameters: List<TypeDefinition>,
							 val parameters: List<Parameter>, body: ErrorHandlingContext?,
							 val returnType: Type?, val isNative: Boolean = false): Unit() {
	val signature = FunctionSignature(source, genericParameters, parameters.map { p -> p.type }, returnType)

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