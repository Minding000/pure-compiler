package components.code_generation.llvm.models.operations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.values.Value
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.operations.BinaryOperator
import components.semantic_model.types.OptionalType
import components.semantic_model.values.Operator
import errors.internal.CompilerError
import java.util.*

class BinaryOperator(override val model: BinaryOperator, val left: Value, val right: Value): Value(model, listOf(left, right)) {

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		if(model.kind == Operator.Kind.DOUBLE_QUESTION_MARK)
			return getNullCoalescenceResult(constructor)
		val resultName = "_binaryOperatorResult"
		var leftValue = ValueConverter.convertIfRequired(model, constructor, left.getLlvmValue(constructor), left.model.effectiveType,
			left.model.hasGenericType, left.model.effectiveType, false)
		val rightType = model.targetSignature?.parameterTypes?.firstOrNull() ?: right.model.effectiveType
		val originalRightType = model.targetSignature?.original?.parameterTypes?.firstOrNull() ?: right.model.effectiveType
		var rightValue = ValueConverter.convertIfRequired(model, constructor, right.getLlvmValue(constructor), right.model.effectiveType,
			right.model.hasGenericType, rightType, rightType != originalRightType, model.conversions?.get(right.model))
		if(SpecialType.BOOLEAN.matches(left.model.effectiveType) && SpecialType.BOOLEAN.matches(right.model.effectiveType)) {
			when(model.kind) {
				Operator.Kind.AND -> return constructor.buildAnd(leftValue, rightValue, resultName)
				Operator.Kind.OR -> return constructor.buildOr(leftValue, rightValue, resultName)
				Operator.Kind.EQUAL_TO -> return constructor.buildBooleanEqualTo(leftValue, rightValue, resultName)
				Operator.Kind.NOT_EQUAL_TO -> return constructor.buildBooleanNotEqualTo(leftValue, rightValue, resultName)
				else -> {}
			}
		}
		val isLeftFloat = SpecialType.FLOAT.matches(left.model.effectiveType)
		val isLeftInteger = SpecialType.INTEGER.matches(left.model.effectiveType)
		val isLeftPrimitiveNumber = SpecialType.BYTE.matches(left.model.effectiveType) || isLeftInteger || isLeftFloat
		val isRightFloat = SpecialType.FLOAT.matches(right.model.effectiveType)
		val isRightInteger = SpecialType.INTEGER.matches(right.model.effectiveType)
		val isRightPrimitiveNumber = SpecialType.BYTE.matches(right.model.effectiveType) || isRightInteger || isRightFloat
		if(isLeftPrimitiveNumber && isRightPrimitiveNumber) {
			val intermediateOperandName = "_implicitlyCastBinaryOperand"
			val isFloatOperation = isLeftFloat || isRightFloat
			val isIntegerOperation = isLeftInteger || isRightInteger
			if(isFloatOperation) {
				if(!isLeftFloat)
					leftValue = constructor.buildCastFromSignedIntegerToFloat(leftValue, intermediateOperandName)
				else if(!isRightFloat)
					rightValue = constructor.buildCastFromSignedIntegerToFloat(rightValue, intermediateOperandName)
			} else if(isIntegerOperation) {
				if(!isLeftInteger)
					leftValue = constructor.buildCastFromByteToInteger(leftValue, intermediateOperandName)
				else if(!isRightInteger)
					rightValue = constructor.buildCastFromByteToInteger(rightValue, intermediateOperandName)
			}
			when(model.kind) {
				Operator.Kind.PLUS -> {
					return if(isFloatOperation) {
						constructor.buildFloatAddition(leftValue, rightValue, resultName)
					} else {
						val function = if(isIntegerOperation)
							context.externalFunctions.si32Addition
						else
							context.externalFunctions.si8Addition
						context.raiseOnOverflow(constructor, model, leftValue, rightValue, function,
							"Addition overflowed", resultName)
					}
				}
				Operator.Kind.MINUS -> {
					return if(isFloatOperation) {
						constructor.buildFloatSubtraction(leftValue, rightValue, resultName)
					} else {
						val function = if(isIntegerOperation)
							context.externalFunctions.si32Subtraction
						else
							context.externalFunctions.si8Subtraction
						context.raiseOnOverflow(constructor, model, leftValue, rightValue, function,
							"Subtraction overflowed", resultName)
					}
				}
				Operator.Kind.STAR -> {
					return if(isFloatOperation) {
						constructor.buildFloatMultiplication(leftValue, rightValue, resultName)
					} else {
						val function = if(isIntegerOperation)
							context.externalFunctions.si32Multiplication
						else
							context.externalFunctions.si8Multiplication
						context.raiseOnOverflow(constructor, model, leftValue, rightValue, function,
							"Multiplication overflowed", resultName)
					}
				}
				Operator.Kind.SLASH -> {
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
					return if(isFloatOperation) {
						//TODO report overflow? (check for infinity)
						val isDivisorZero = constructor.buildFloatEqualTo(rightValue, constructor.buildFloat(0.0), "isDivisorZero")
						constructor.buildJump(isDivisorZero, divisionByZeroBlock, noDivisionByZeroBlock)
						constructor.select(noDivisionByZeroBlock)
						constructor.buildFloatDivision(leftValue, rightValue, resultName)
					} else {
						val zero = if(isIntegerOperation)
							constructor.buildInt32(0)
						else
							constructor.buildByte(0)
						val isDivisorZero = constructor.buildSignedIntegerEqualTo(rightValue, zero, "isDivisorZero")
						constructor.buildJump(isDivisorZero, divisionByZeroBlock, noDivisionByZeroBlock)
						constructor.select(noDivisionByZeroBlock)
						val noOverflowBlock = constructor.createBlock("noOverflow")
						val overflowBlock = constructor.createBlock("overflow")
						val negativeMin = if(isIntegerOperation)
							constructor.buildInt32(Int.MIN_VALUE)
						else
							constructor.buildByte(Byte.MIN_VALUE)
						val isDividendNegativeMin = constructor.buildSignedIntegerEqualTo(leftValue, negativeMin, "isDividendNegativeMin")
						val negativeOne = if(isIntegerOperation)
							constructor.buildInt32(-1)
						else
							constructor.buildByte(-1)
						val isDivisorNegativeOne = constructor.buildSignedIntegerEqualTo(rightValue, negativeOne, "isDivisorNegativeOne")
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
						constructor.buildSignedIntegerDivision(leftValue, rightValue, resultName)
					}
				}
				Operator.Kind.SMALLER_THAN -> {
					return if(isFloatOperation)
						constructor.buildFloatLessThan(leftValue, rightValue, resultName)
					else
						constructor.buildSignedIntegerLessThan(leftValue, rightValue, resultName)
				}
				Operator.Kind.GREATER_THAN -> {
					return if(isFloatOperation)
						constructor.buildFloatGreaterThan(leftValue, rightValue, resultName)
					else
						constructor.buildSignedIntegerGreaterThan(leftValue, rightValue, resultName)
				}
				Operator.Kind.SMALLER_THAN_OR_EQUAL_TO -> {
					return if(isFloatOperation)
						constructor.buildFloatLessThanOrEqualTo(leftValue, rightValue, resultName)
					else
						constructor.buildSignedIntegerLessThanOrEqualTo(leftValue, rightValue, resultName)
				}
				Operator.Kind.GREATER_THAN_OR_EQUAL_TO -> {
					return if(isFloatOperation)
						constructor.buildFloatGreaterThanOrEqualTo(leftValue, rightValue, resultName)
					else
						constructor.buildSignedIntegerGreaterThanOrEqualTo(leftValue, rightValue, resultName)
				}
				Operator.Kind.EQUAL_TO -> {
					return if(isFloatOperation)
						constructor.buildFloatEqualTo(leftValue, rightValue, resultName)
					else
						constructor.buildSignedIntegerEqualTo(leftValue, rightValue, resultName)
				}
				Operator.Kind.NOT_EQUAL_TO -> {
					return if(isFloatOperation)
						constructor.buildFloatNotEqualTo(leftValue, rightValue, resultName)
					else
						constructor.buildSignedIntegerNotEqualTo(leftValue, rightValue, resultName)
				}
				else -> {}
			}
		}
		if(model.kind == Operator.Kind.EQUAL_TO) {
			if(SpecialType.NULL.matches(left.model.effectiveType)) {
				return constructor.buildPointerEqualTo(constructor.nullPointer, rightValue, resultName)
			} else if(left.model.effectiveType is OptionalType) {
				val resultVariable = constructor.buildStackAllocation(constructor.booleanType, "_resultVariable")
				val isLeftNull = constructor.buildPointerEqualTo(constructor.nullPointer, leftValue, "_isLeftNull")
				val leftNullBlock = constructor.createBlock("leftNull")
				val leftNotNullBlock = constructor.createBlock("leftNotNull")
				val resultBlock = constructor.createBlock("result")
				constructor.buildJump(isLeftNull, leftNullBlock, leftNotNullBlock)
				constructor.select(leftNullBlock)
				val isRightNull = constructor.buildPointerEqualTo(constructor.nullPointer, rightValue, "_isRightNull")
				constructor.buildStore(isRightNull, resultVariable)
				constructor.buildJump(resultBlock)
				constructor.select(leftNotNullBlock)
				val result = createLlvmFunctionCall(constructor, leftValue, rightValue)
				constructor.buildStore(result, resultVariable)
				constructor.buildJump(resultBlock)
				constructor.select(resultBlock)
				return constructor.buildLoad(constructor.booleanType, resultVariable, resultName)
			}
		} else if(model.kind == Operator.Kind.NOT_EQUAL_TO) {
			if(SpecialType.NULL.matches(left.model.effectiveType)) {
				return constructor.buildPointerNotEqualTo(constructor.nullPointer, rightValue, resultName)
			} else if(left.model.effectiveType is OptionalType) {
				val resultVariable = constructor.buildStackAllocation(constructor.booleanType, "_resultVariable")
				val isLeftNull = constructor.buildPointerEqualTo(constructor.nullPointer, leftValue, "_isLeftNull")
				val leftNullBlock = constructor.createBlock("leftNull")
				val leftNotNullBlock = constructor.createBlock("leftNotNull")
				val resultBlock = constructor.createBlock("result")
				constructor.buildJump(isLeftNull, leftNullBlock, leftNotNullBlock)
				constructor.select(leftNullBlock)
				val isRightNotNull = constructor.buildPointerNotEqualTo(constructor.nullPointer, rightValue, "_isRightNull")
				constructor.buildStore(isRightNotNull, resultVariable)
				constructor.buildJump(resultBlock)
				constructor.select(leftNotNullBlock)
				val result = createLlvmFunctionCall(constructor, leftValue, rightValue)
				constructor.buildStore(result, resultVariable)
				constructor.buildJump(resultBlock)
				constructor.select(resultBlock)
				return constructor.buildLoad(constructor.booleanType, resultVariable, resultName)
			}
		}
		return createLlvmFunctionCall(constructor, leftValue, rightValue)
	}

	private fun createLlvmFunctionCall(constructor: LlvmConstructor, leftValue: LlvmValue, rightValue: LlvmValue): LlvmValue {
		val signature = model.targetSignature?.original ?: throw CompilerError(model, "Binary operator is missing a target.")
		val resultName = "_binaryOperatorResult"
		val parameters = LinkedList<LlvmValue>()
		parameters.add(context.getExceptionParameter(constructor))
		parameters.add(leftValue)
		parameters.add(rightValue)
		val typeDefinition = signature.parentTypeDeclaration
		val returnValue = if(typeDefinition?.isLlvmPrimitive() == true) {
			val primitiveImplementation = context.nativeRegistry.resolvePrimitiveImplementation(
				"${typeDefinition.name}${signature.original.toString(false, model.kind)}")
			constructor.buildFunctionCall(primitiveImplementation.llvmType, primitiveImplementation.llvmValue, parameters, resultName)
		} else {
			val functionAddress = context.resolveFunction(constructor, leftValue, signature.getIdentifier(model.kind))
			constructor.buildFunctionCall(signature.getLlvmType(constructor), functionAddress, parameters, resultName)
		}
		context.continueRaise(constructor, model)
		return returnValue
	}

	private fun getNullCoalescenceResult(constructor: LlvmConstructor): LlvmValue {
		val rightValue = right.getLlvmValue(constructor)
		if(SpecialType.NULL.matches(left.model.providedType))
			return rightValue
		var leftValue = left.getLlvmValue(constructor)
		val leftType = left.model.providedType
		if(leftType !is OptionalType)
			return leftValue
		val resultType = model.providedType?.getLlvmType(constructor)
		val result = constructor.buildStackAllocation(resultType, "_nullCoalescence_resultVariable")
		val function = constructor.getParentFunction()
		val valueBlock = constructor.createBlock(function, "nullCoalescence_valueBlock")
		val nullBlock = constructor.createBlock(function, "nullCoalescence_nullBlock")
		val resultBlock = constructor.createBlock(function, "nullCoalescence_resultBlock")
		constructor.buildJump(constructor.buildIsNull(leftValue, "_nullCoalescence_isLeftNull"), nullBlock, valueBlock)
		constructor.select(nullBlock)
		constructor.buildStore(rightValue, result)
		constructor.buildJump(resultBlock)
		constructor.select(valueBlock)
		if(model.providedType?.isLlvmPrimitive() == true && leftType.baseType.isLlvmPrimitive())
			leftValue = constructor.buildLoad(leftType.baseType.getLlvmType(constructor), leftValue, "_unboxedPrimitive")
		constructor.buildStore(leftValue, result)
		constructor.buildJump(resultBlock)
		constructor.select(resultBlock)
		return constructor.buildLoad(resultType, result, "_nullCoalescence_result")
	}
}
