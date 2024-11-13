package components.code_generation.llvm.models.values

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.values.BooleanLiteral

class BooleanLiteral(override val model: BooleanLiteral): Value(model) {

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		return constructor.buildBoolean(model.value)
	}
}
