package components.code_generation.llvm.models.control_flow

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.general.Unit
import components.code_generation.llvm.models.values.Value
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.semantic_model.control_flow.ReturnStatement

class ReturnStatement(override val model: ReturnStatement, val value: Value?): Unit(model, listOfNotNull(value)) {

	override fun compile(constructor: LlvmConstructor) {
		val errorHandlingContext = model.scope.getSurroundingAlwaysBlock()?.unit
		if(value == null) {
			errorHandlingContext?.runAlwaysBlock(constructor)
			constructor.buildReturn()
			return
		}
		val targetFunction = model.targetFunction
		val targetComputedProperty = model.targetComputedProperty
		val declaredReturnType = targetFunction?.signature?.returnType ?: targetComputedProperty?.getterReturnType
		val isTargetTypeGeneric = targetFunction?.signature?.hasGenericType ?: (targetComputedProperty?.hasGenericType ?: false)
		val returnValue = ValueConverter.convertIfRequired(model, constructor, value.getLlvmValue(constructor), value.model.effectiveType,
			value.model.hasGenericType, declaredReturnType, isTargetTypeGeneric, model.conversion)
		errorHandlingContext?.runAlwaysBlock(constructor)
		constructor.buildReturn(returnValue)
	}
}
