package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context

object IdentifiableNatives {
	lateinit var context: Context

	fun load(context: Context) {
		this.context = context
		context.registerNativeImplementation("Identifiable === Any?: Bool", ::identicalTo)
	}

	private fun identicalTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectBlock(llvmFunctionValue, "entrypoint")
		val thisValue = context.getThisParameter(constructor)
		val parameterValue = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildSignedIntegerEqualTo(thisValue, parameterValue, "_equalsResult")
		constructor.buildReturn(result)
	}
}
