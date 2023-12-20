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
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val string = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val bytesProperty = context.resolveMember(constructor, context.stringTypeDeclaration?.llvmType, string, "bytes")
		val byteArray = constructor.buildLoad(constructor.pointerType, bytesProperty, "byteArray")
		val arrayValueProperty = constructor.buildGetPropertyPointer(context.arrayTypeDeclaration?.llvmType, byteArray,
			context.arrayValueIndex, "arrayValueProperty")
		val primitiveArray = constructor.buildLoad(constructor.pointerType, arrayValueProperty, "primitiveArray")
		constructor.buildFunctionCall(context.llvmPrintFunctionType, context.llvmPrintFunction, listOf(primitiveArray))
		constructor.buildFunctionCall(context.llvmFlushFunctionType, context.llvmFlushFunction, listOf(constructor.nullPointer))
		constructor.buildReturn()
	}
}
