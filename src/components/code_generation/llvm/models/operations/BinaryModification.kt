package components.code_generation.llvm.models.operations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.general.Unit
import components.code_generation.llvm.models.values.Value
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.operations.BinaryModification
import components.semantic_model.values.Operator
import errors.internal.CompilerError
import java.util.*

class BinaryModification(override val model: BinaryModification, val target: Value, val modifier: Value):
	Unit(model, listOf(target, modifier)) {

	//TODO test optional target (also for other operators)
	override fun compile(constructor: LlvmConstructor) {
		val targetValue = ValueConverter.convertIfRequired(model, constructor, target.getLlvmValue(constructor),
			target.model.effectiveType, target.model.hasGenericType, target.model.effectiveType, false)
		val modifierType = model.targetSignature?.parameterTypes?.firstOrNull() ?: modifier.model.effectiveType
		val originalModifierType = model.targetSignature?.original?.parameterTypes?.firstOrNull() ?: modifier.model.effectiveType
		var modifierValue = ValueConverter.convertIfRequired(model, constructor, modifier.getLlvmValue(constructor),
			modifier.model.effectiveType, modifier.model.hasGenericType, modifierType, modifierType != originalModifierType,
			model.conversions?.get(modifier.model))
		val isTargetFloat = SpecialType.FLOAT.matches(target.model.effectiveType)
		val isTargetInteger = SpecialType.INTEGER.matches(target.model.effectiveType)
		val isTargetPrimitiveNumber = SpecialType.BYTE.matches(target.model.effectiveType) || isTargetInteger || isTargetFloat
		val isModifierFloat = SpecialType.FLOAT.matches(modifier.model.effectiveType)
		val isModifierInteger = SpecialType.INTEGER.matches(modifier.model.effectiveType)
		val isModifierPrimitiveNumber = SpecialType.BYTE.matches(modifier.model.effectiveType) || isModifierInteger || isModifierFloat
		if(isTargetPrimitiveNumber && isModifierPrimitiveNumber) {
			val isFloatOperation = isTargetFloat || isModifierFloat
			val isIntegerOperation = isTargetInteger || isModifierInteger
			if(isFloatOperation) {
				if(isTargetInteger)
					throw CompilerError(model, "Integer target with float modifier in binary modification.")
				else if(isModifierInteger)
					modifierValue = constructor.buildCastFromSignedIntegerToFloat(modifierValue, "_implicitlyCastBinaryModifier")
			} else if(isIntegerOperation) {
				if(!isTargetInteger)
					throw CompilerError(model, "Byte target with integer modifier in binary modification.")
				else if(!isModifierInteger)
					modifierValue = constructor.buildCastFromByteToInteger(modifierValue, "_implicitlyCastBinaryModifier")
			}
			val intermediateResultName = "_modifiedValue"
			val operation = when(model.kind) {
				Operator.Kind.PLUS_EQUALS -> {
					if(isFloatOperation) {
						constructor.buildFloatAddition(targetValue, modifierValue, intermediateResultName)
					} else {
						val function = if(isIntegerOperation)
							context.externalFunctions.si32Addition
						else
							context.externalFunctions.si8Addition
						context.raiseOnOverflow(constructor, model, targetValue, modifierValue, function,
							"Addition overflowed", intermediateResultName)
					}
				}
				Operator.Kind.MINUS_EQUALS -> {
					if(isFloatOperation) {
						constructor.buildFloatSubtraction(targetValue, modifierValue, intermediateResultName)
					} else {
						val function = if(isIntegerOperation)
							context.externalFunctions.si32Subtraction
						else
							context.externalFunctions.si8Subtraction
						context.raiseOnOverflow(constructor, model, targetValue, modifierValue, function,
							"Subtraction overflowed", intermediateResultName)
					}
				}
				Operator.Kind.STAR_EQUALS -> {
					if(isFloatOperation) {
						constructor.buildFloatMultiplication(targetValue, modifierValue, intermediateResultName)
					} else {
						val function = if(isIntegerOperation)
							context.externalFunctions.si32Multiplication
						else
							context.externalFunctions.si8Multiplication
						context.raiseOnOverflow(constructor, model, targetValue, modifierValue, function,
							"Multiplication overflowed", intermediateResultName)
					}
				}
				Operator.Kind.SLASH_EQUALS -> {
					val noDivisionByZeroBlock = constructor.createBlock("validDivision")
					val divisionByZeroBlock = constructor.createBlock("divisionByZero")
					val previousBlock = constructor.getCurrentBlock()
					constructor.select(divisionByZeroBlock)
					if(context.nativeRegistry.has(SpecialType.EXCEPTION)) {
						context.raiseException(constructor, model, "Division by zero")
					} else {
						context.panic(constructor, "Division by zero")
						constructor.markAsUnreachable()
					}
					constructor.select(previousBlock)
					if(isFloatOperation) {
						val isDivisorZero = constructor.buildFloatEqualTo(modifierValue, constructor.buildFloat(0.0), "isDivisorZero")
						constructor.buildJump(isDivisorZero, divisionByZeroBlock, noDivisionByZeroBlock)
						constructor.select(noDivisionByZeroBlock)
						constructor.buildFloatDivision(targetValue, modifierValue, intermediateResultName)
					} else {
						val zero = if(isIntegerOperation)
							constructor.buildInt32(0)
						else
							constructor.buildByte(0)
						val isDivisorZero = constructor.buildSignedIntegerEqualTo(modifierValue, zero, "isDivisorZero")
						constructor.buildJump(isDivisorZero, divisionByZeroBlock, noDivisionByZeroBlock)
						constructor.select(noDivisionByZeroBlock)
						val noOverflowBlock = constructor.createBlock("noOverflow")
						val overflowBlock = constructor.createBlock("overflow")
						val negativeMin = if(isIntegerOperation)
							constructor.buildInt32(Int.MIN_VALUE)
						else
							constructor.buildByte(Byte.MIN_VALUE)
						val isDividendNegativeMin = constructor.buildSignedIntegerEqualTo(targetValue, negativeMin, "isDividendNegativeMin")
						val negativeOne = if(isIntegerOperation)
							constructor.buildInt32(-1)
						else
							constructor.buildByte(-1)
						val isDivisorNegativeOne = constructor.buildSignedIntegerEqualTo(modifierValue, negativeOne, "isDivisorNegativeOne")
						val doesDivisionOverflow = constructor.buildAnd(isDividendNegativeMin, isDivisorNegativeOne, "doesDivisionOverflow")
						constructor.buildJump(doesDivisionOverflow, overflowBlock, noOverflowBlock)
						constructor.select(overflowBlock)
						if(context.nativeRegistry.has(SpecialType.EXCEPTION)) {
							context.raiseException(constructor, model, "Division overflowed")
						} else {
							context.panic(constructor, "Division overflowed")
							constructor.markAsUnreachable()
						}
						constructor.select(noOverflowBlock)
						constructor.buildSignedIntegerDivision(targetValue, modifierValue, intermediateResultName)
					}
				}
				else -> throw CompilerError(model, "Unknown native unary integer modification of kind '${model.kind}'.")
			}
			constructor.buildStore(ValueConverter.convertIfRequired(model, constructor, operation, target.model.effectiveType,
				false, target.model.effectiveType, target.model.hasGenericType), target.getLlvmLocation(constructor))
			return
		}
		val signature = model.targetSignature?.original ?: throw CompilerError(model, "Binary modification is missing a target.")
		createLlvmFunctionCall(constructor, signature, targetValue, modifierValue)
	}

	private fun createLlvmFunctionCall(constructor: LlvmConstructor, signature: FunctionSignature, targetValue: LlvmValue,
									   modifierValue: LlvmValue) {
		val parameters = LinkedList<LlvmValue>()
		parameters.add(context.getExceptionParameter(constructor))
		parameters.add(targetValue)
		parameters.add(modifierValue)
		val functionAddress = context.resolveFunction(constructor, targetValue, signature.getIdentifier(model.kind))
		constructor.buildFunctionCall(signature.getLlvmType(constructor), functionAddress, parameters)
		context.continueRaise(constructor, model)
	}
}
