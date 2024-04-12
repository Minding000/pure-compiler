package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.NativeRegistry

class IdentifiableNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativeImplementation("Identifiable === Any?: Bool", ::identicalTo)
	}

	private fun identicalTo(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val thisIdentifiable = context.getThisParameter(constructor)
		val parameterAny = constructor.getParameter(llvmFunctionValue, Context.VALUE_PARAMETER_OFFSET)
		val result = constructor.buildPointerEqualTo(thisIdentifiable, parameterAny, "equalsResult")
		constructor.buildReturn(result)
	}
}
