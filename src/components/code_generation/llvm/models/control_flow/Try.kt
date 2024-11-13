package components.code_generation.llvm.models.control_flow

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.values.Value
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.control_flow.Try

class Try(override val model: Try, val expression: Value): Value(model, listOf(expression)) {

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val expressionResult = expression.getLlvmValue(constructor)
		return if(model.isOptional) {
			val exceptionParameter = context.getExceptionParameter(constructor)
			if(SpecialType.NOTHING.matches(expression.model.effectiveType)) {
				constructor.buildStore(constructor.nullPointer, exceptionParameter)
				return expressionResult
			}
			val exception = constructor.buildLoad(constructor.pointerType, exceptionParameter, "exception")
			val doesExceptionExist = constructor.buildIsNotNull(exception, "doesExceptionExist")
			val resultType = model.effectiveType?.getLlvmType(constructor)
			val resultVariable = constructor.buildStackAllocation(resultType, "resultVariable")
			val exceptionBlock = constructor.createBlock("try_exception")
			val noExceptionBlock = constructor.createBlock("try_noException")
			val resultBlock = constructor.createBlock("try_result")
			constructor.buildJump(doesExceptionExist, exceptionBlock, noExceptionBlock)
			constructor.select(exceptionBlock)
			constructor.buildStore(constructor.nullPointer, exceptionParameter)
			constructor.buildStore(constructor.nullPointer, resultVariable)
			constructor.buildJump(resultBlock)
			constructor.select(noExceptionBlock)
			constructor.buildStore(ValueConverter.convertIfRequired(model, constructor, expressionResult, expression.model.effectiveType,
				expression.model.hasGenericType, model.effectiveType, false), resultVariable)
			constructor.buildJump(resultBlock)
			constructor.select(resultBlock)
			constructor.buildLoad(resultType, resultVariable, "result")
		} else {
			expressionResult
		}
	}
}
