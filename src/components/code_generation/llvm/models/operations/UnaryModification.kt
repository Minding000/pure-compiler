package components.code_generation.llvm.models.operations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.general.Unit
import components.code_generation.llvm.models.values.Value
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.operations.UnaryModification
import components.semantic_model.values.Operator
import errors.internal.CompilerError
import java.util.*

class UnaryModification(override val model: UnaryModification, val target: Value): Unit(model, listOf(target)) {

	override fun compile(constructor: LlvmConstructor) {
		val targetValue = ValueConverter.convertIfRequired(model, constructor, target.getLlvmValue(constructor),
			target.model.effectiveType, target.model.hasGenericType, target.model.effectiveType, false)
		val isTargetInteger = SpecialType.INTEGER.matches(target.model.providedType)
		if(isTargetInteger || SpecialType.BYTE.matches(target.model.providedType)) {
			val modifierValue = if(isTargetInteger)
				constructor.buildInt32(UnaryModification.STEP_SIZE.longValueExact())
			else
				constructor.buildByte(UnaryModification.STEP_SIZE.longValueExact())
			val intermediateResultName = "_modifiedValue"
			val operation = when(model.kind) {
				Operator.Kind.DOUBLE_PLUS -> constructor.buildIntegerAddition(targetValue, modifierValue, intermediateResultName)
				Operator.Kind.DOUBLE_MINUS -> constructor.buildIntegerSubtraction(targetValue, modifierValue, intermediateResultName)
				else -> throw CompilerError(model, "Unknown native unary integer modification of kind '${model.kind}'.")
			}
			constructor.buildStore(ValueConverter.convertIfRequired(model, constructor, operation, target.model.effectiveType,
				false, target.model.effectiveType, target.model.hasGenericType), target.getLlvmLocation(constructor))
			return
		}
		val signature = model.targetSignature?.original ?: throw CompilerError(model, "Unary modification is missing a target.")
		createLlvmFunctionCall(constructor, signature, targetValue)
	}

	private fun createLlvmFunctionCall(constructor: LlvmConstructor, signature: FunctionSignature, targetValue: LlvmValue) {
		val parameters = LinkedList<LlvmValue>()
		parameters.add(context.getExceptionParameter(constructor))
		parameters.add(targetValue)
		val functionAddress = context.resolveFunction(constructor, targetValue, signature.getIdentifier(model.kind))
		constructor.buildFunctionCall(signature.getLlvmType(constructor), functionAddress, parameters)
		context.continueRaise(constructor, model)
	}
}
