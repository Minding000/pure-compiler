package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.NativeRegistry

class NullNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativeImplementation("Null == Any?: Bool", ::equalTo)
	}

	private fun equalTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisNull = context.getThisParameter(constructor)
		val parameterAny = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildSignedIntegerEqualTo(thisNull, parameterAny, "equalsResult")
		constructor.buildReturn(result)
	}
}
