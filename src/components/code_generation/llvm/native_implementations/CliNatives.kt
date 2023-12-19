package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context

object CliNatives {
	lateinit var context: Context

	fun load(context: Context) {
		this.context = context
		context.registerNativeImplementation("Cli.writeLine(String)", ::writeLine)
	}

	private fun writeLine(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val string = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val bytesProperty = context.resolveMember(constructor, context.stringTypeDeclaration?.llvmType, string, "bytes")
		val bytesPointer = constructor.buildLoad(constructor.pointerType, bytesProperty, "bytesPointer")
		val arrayProperty = constructor.buildGetPropertyPointer(context.arrayTypeDeclaration?.llvmType, bytesPointer,
			context.arrayValueIndex, "_arrayProperty")
		val arrayPointer = constructor.buildLoad(constructor.pointerType, arrayProperty, "arrayPointer")
		constructor.buildFunctionCall(context.llvmPrintFunctionType, context.llvmPrintFunction, listOf(arrayPointer), "_ignore")
		constructor.buildFunctionCall(context.llvmFlushFunctionType, context.llvmFlushFunction, listOf(constructor.nullPointer),
			"_ignore")
		constructor.buildReturn()
	}
}
