package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.context.NativeRegistry
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context

class MathNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativeImplementation("Math.getRemainder(Int, Int): Int", ::getRemainder)
	}

	private fun getRemainder(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val dividend = constructor.getParameter(Context.VALUE_PARAMETER_OFFSET)
		val divisor = constructor.getParameter(Context.VALUE_PARAMETER_OFFSET + 1)
		constructor.buildReturn(constructor.buildSignedIntegerRemainder(dividend, divisor, "remainder"))
	}
}
