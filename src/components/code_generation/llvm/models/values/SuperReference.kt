package components.code_generation.llvm.models.values

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.values.SuperReference

class SuperReference(override val model: SuperReference): Value(model) {

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		return context.getThisParameter(constructor)
	}
}
