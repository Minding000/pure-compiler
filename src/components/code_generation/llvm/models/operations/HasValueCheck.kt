package components.code_generation.llvm.models.operations

import components.code_generation.llvm.models.values.Value
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.operations.HasValueCheck
import components.semantic_model.types.OptionalType

class HasValueCheck(override val model: HasValueCheck, val subject: Value): Value(model, listOf(subject)) {

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		if(SpecialType.NULL.matches(subject.model.effectiveType))
			return constructor.buildBoolean(false)
		if(subject.model.effectiveType !is OptionalType)
			return constructor.buildBoolean(true)
		return constructor.buildIsNotNull(subject.getLlvmValue(constructor), "_hasValueCheck_result")
	}
}
