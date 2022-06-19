package linter.elements.definitions

import linter.Linter
import linter.elements.general.Unit
import linter.elements.literals.Type
import linter.elements.values.VariableValueDeclaration
import linter.scopes.BlockScope
import linter.scopes.Scope
import parsing.ast.definitions.FunctionDefinition

class FunctionDefinition(override val source: FunctionDefinition, name: String, val scope: BlockScope,
						 val genericParameters: List<Unit>, val parameters: List<Parameter>, val body: Unit?,
						 val returnType: Type?, val isNative: Boolean):
	VariableValueDeclaration(source, name, returnType, true) {
	val variation = parameters.joinToString { parameter -> parameter.type.toString() }

	init {
		units.addAll(genericParameters)
		units.addAll(parameters)
		if(body != null)
			units.add(body)
		if(returnType != null)
			units.add(returnType)
	}

	override fun linkReferences(linter: Linter, scope: Scope) {
		super.linkReferences(linter, this.scope)
	}

	override fun toString(): String {
		return "$name($variation)"
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