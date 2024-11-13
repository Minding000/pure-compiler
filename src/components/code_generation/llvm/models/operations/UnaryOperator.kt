package components.code_generation.llvm.models.operations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.values.NumberLiteral
import components.code_generation.llvm.models.values.Value
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.operations.UnaryOperator
import components.semantic_model.values.Operator
import errors.internal.CompilerError
import java.util.*

class UnaryOperator(override val model: UnaryOperator, val subject: Value): Value(model, listOf(subject)) {

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		if(model.kind == Operator.Kind.MINUS && subject is NumberLiteral)
			return subject.createLlvmValue(constructor, subject.model.value.negate())
		val resultName = "_unaryOperatorResult"
		val llvmValue = ValueConverter.convertIfRequired(model, constructor, subject.getLlvmValue(constructor), subject.model.effectiveType,
			subject.model.hasGenericType, subject.model.effectiveType, false)
		if(SpecialType.BOOLEAN.matches(subject.model.providedType)) {
			if(model.kind == Operator.Kind.EXCLAMATION_MARK)
				return constructor.buildBooleanNegation(llvmValue, resultName)
		} else if(SpecialType.BYTE.matches(subject.model.providedType) || SpecialType.INTEGER.matches(subject.model.providedType)) {
			if(model.kind == Operator.Kind.MINUS)
				return constructor.buildIntegerNegation(llvmValue, resultName)
		} else if(SpecialType.FLOAT.matches(subject.model.providedType)) {
			if(model.kind == Operator.Kind.MINUS)
				return constructor.buildFloatNegation(llvmValue, resultName)
		}
		val signature = model.targetSignature?.original ?: throw CompilerError(model, "Unary operator is missing a target.")
		return createLlvmFunctionCall(constructor, signature, llvmValue)
	}

	private fun createLlvmFunctionCall(constructor: LlvmConstructor, signature: FunctionSignature, targetValue: LlvmValue): LlvmValue {
		val parameters = LinkedList<LlvmValue>()
		parameters.add(context.getExceptionParameter(constructor))
		parameters.add(targetValue)
		val functionAddress = context.resolveFunction(constructor, targetValue, signature.getIdentifier(model.kind))
		val returnValue = constructor.buildFunctionCall(signature.getLlvmType(constructor), functionAddress, parameters,
			"_unaryOperatorResult")
		context.continueRaise(constructor, model)
		return returnValue
	}
}
