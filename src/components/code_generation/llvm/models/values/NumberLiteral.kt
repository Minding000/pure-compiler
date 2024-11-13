package components.code_generation.llvm.models.values

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.values.NumberLiteral
import java.math.BigDecimal

class NumberLiteral(override val model: NumberLiteral): Value(model) {

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		return createLlvmValue(constructor, model.value)
	}

	fun createLlvmValue(constructor: LlvmConstructor, value: BigDecimal): LlvmValue {
		return if(SpecialType.BYTE.matches(model.effectiveType))
			constructor.buildByte(value.longValueExact())
		else if(SpecialType.INTEGER.matches(model.effectiveType))
			constructor.buildInt32(value.longValueExact())
		else
			constructor.buildFloat(value.toDouble())
	}
}
