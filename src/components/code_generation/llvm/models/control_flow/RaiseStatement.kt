package components.code_generation.llvm.models.control_flow

import components.code_generation.llvm.models.general.Unit
import components.code_generation.llvm.models.values.Value
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.semantic_model.control_flow.RaiseStatement
import errors.internal.CompilerError

class RaiseStatement(override val model: RaiseStatement, val value: Value): Unit(model, listOf(value)) {

	override fun compile(constructor: LlvmConstructor) {
		val exceptionParameter = context.getExceptionParameter(constructor)
		val exception = value.getLlvmValue(constructor)
		val surroundingCallable = model.targetInitializer ?: model.targetFunction ?: model.targetComputedProperty
		?: throw CompilerError(model, "Missing target callable.")
		context.addLocationToStacktrace(model, constructor, exception, surroundingCallable)
		constructor.buildStore(exception, exceptionParameter)
		context.handleException(constructor, model.parent)
	}
}
