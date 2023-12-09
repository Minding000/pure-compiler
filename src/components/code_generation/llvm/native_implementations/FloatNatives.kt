package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context

object FloatNatives {
	lateinit var context: Context

	fun load(context: Context) {
		this.context = context
		context.nativeImplementations["Float + Self: Self"] = ::plus
	}

	private fun plus(constructor: LlvmConstructor, llvmValue: LlvmValue) {
		//TODO implement
	}
}
