package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context

object NullNatives {
	lateinit var context: Context

	fun load(context: Context) {
		this.context = context
		context.registerNativeImplementation("Null == Any: Bool", ::equalTo)
	}

	private fun equalTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		constructor.buildReturn(constructor.buildBoolean(false))
	}
}
