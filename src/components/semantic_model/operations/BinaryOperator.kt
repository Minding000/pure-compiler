package components.semantic_model.operations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.scopes.Scope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.OptionalType
import components.semantic_model.values.*
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.access.AbstractMonomorphicAccess
import logger.issues.access.WhereClauseUnfulfilled
import logger.issues.resolution.NotFound
import util.combineOrUnion
import java.util.*
import components.syntax_parser.syntax_tree.operations.BinaryOperator as BinaryOperatorSyntaxTree

class BinaryOperator(override val source: BinaryOperatorSyntaxTree, scope: Scope, val left: Value, val right: Value,
					 val kind: Operator.Kind): Value(source, scope) {
	var targetSignature: FunctionSignature? = null
	var conversions: Map<Value, InitializerDefinition>? = null
	override val hasGenericType: Boolean
		get() = targetSignature?.original?.returnType != targetSignature?.returnType

	init {
		addSemanticModels(left, right)
	}

	override fun determineTypes() {
		super.determineTypes()
		var leftType = left.effectiveType ?: return
		val rightType = right.effectiveType ?: return
		if(kind == Operator.Kind.DOUBLE_QUESTION_MARK) {
			if(SpecialType.NULL.matches(leftType)) {
				providedType = rightType
			} else {
				val nonOptionalLeftType = if(leftType is OptionalType) leftType.baseType else leftType
				providedType = listOf(nonOptionalLeftType, rightType).combineOrUnion(this)
				addSemanticModels(providedType)
			}
			return
		}
		if(kind == Operator.Kind.EQUAL_TO || kind == Operator.Kind.NOT_EQUAL_TO || kind == Operator.Kind.IDENTICAL_TO
			|| kind == Operator.Kind.NOT_IDENTICAL_TO) {
			//TODO fix: comparing with null as target leads to crash (write tests!)
			if(leftType is OptionalType)
				leftType = leftType.baseType
		}
		try {
			val match = leftType.interfaceScope.getOperator(kind, right)
			if(match == null) {
				context.addIssue(NotFound(source, "Operator", "$leftType $kind ${right.providedType}"))
				return
			}
			targetSignature = match.signature
			conversions = match.conversions
			setUnextendedType(match.returnType.getLocalType(this, leftType))
		} catch(error: SignatureResolutionAmbiguityError) {
			//TODO write test for this
			error.log(source, "operator", "$leftType $kind ${right.providedType}")
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		val isConditional = SpecialType.BOOLEAN.matches(left.providedType) && (kind == Operator.Kind.AND || kind == Operator.Kind.OR)
		val isComparison = kind == Operator.Kind.EQUAL_TO || kind == Operator.Kind.NOT_EQUAL_TO
		left.analyseDataFlow(tracker)
		if(isConditional) {
			val isAnd = kind == Operator.Kind.AND
			tracker.setVariableStates(left.getEndState(isAnd))
			val variableValue = left as? VariableValue
			val declaration = variableValue?.declaration
			if(declaration != null) {
				val booleanLiteral = BooleanLiteral(this, isAnd)
				tracker.add(VariableUsage.Kind.HINT, declaration, this, booleanLiteral.providedType, booleanLiteral)
			}
			right.analyseDataFlow(tracker)
			setEndState(right.getEndState(isAnd), isAnd)
			tracker.setVariableStates(left.getEndState(!isAnd))
			tracker.addVariableStates(right.getEndState(!isAnd))
			setEndState(tracker, !isAnd)
			tracker.addVariableStates(getEndState(isAnd))
		} else if(isComparison) {
			right.analyseDataFlow(tracker)
			val variableValue = left as? VariableValue ?: right as? VariableValue
			val literalValue = left as? LiteralValue ?: right as? LiteralValue
			val declaration = variableValue?.declaration
			if(declaration != null && literalValue != null) {
				val isPositive = kind == Operator.Kind.EQUAL_TO
				setEndState(tracker, !isPositive)
				tracker.add(VariableUsage.Kind.HINT, declaration, this, literalValue.providedType, literalValue)
				setEndState(tracker, isPositive)
				tracker.addVariableStates(getEndState(!isPositive))
			} else {
				setEndStates(tracker)
			}
		} else {
			right.analyseDataFlow(tracker)
			setEndStates(tracker)
		}
		computeStaticValue()
	}

	private fun computeStaticValue() {
		staticValue = when(kind) {
			Operator.Kind.DOUBLE_QUESTION_MARK -> {
				val leftValue = left.getComputedValue() ?: return
				if(leftValue is NullLiteral)
					right.getComputedValue()
				else
					leftValue
			}
			Operator.Kind.AND -> {
				val leftValue = left.getComputedValue() as? BooleanLiteral ?: return
				val rightValue = right.getComputedValue() as? BooleanLiteral ?: return
				BooleanLiteral(this, leftValue.value && rightValue.value)
			}
			Operator.Kind.OR -> {
				val leftValue = left.getComputedValue() as? BooleanLiteral ?: return
				val rightValue = right.getComputedValue() as? BooleanLiteral ?: return
				BooleanLiteral(this, leftValue.value || rightValue.value)
			}
			Operator.Kind.PLUS -> {
				val leftValue = left.getComputedValue() as? NumberLiteral ?: return
				val rightValue = right.getComputedValue() as? NumberLiteral ?: return
				NumberLiteral(this, leftValue.value + rightValue.value)
			}
			Operator.Kind.MINUS -> {
				val leftValue = left.getComputedValue() as? NumberLiteral ?: return
				val rightValue = right.getComputedValue() as? NumberLiteral ?: return
				NumberLiteral(this, leftValue.value - rightValue.value)
			}
			Operator.Kind.STAR -> {
				val leftValue = left.getComputedValue() as? NumberLiteral ?: return
				val rightValue = right.getComputedValue() as? NumberLiteral ?: return
				NumberLiteral(this, leftValue.value * rightValue.value)
			}
			Operator.Kind.SLASH -> {
				val leftValue = left.getComputedValue() as? NumberLiteral ?: return
				val rightValue = right.getComputedValue() as? NumberLiteral ?: return
				NumberLiteral(this, leftValue.value / rightValue.value)
			}
			Operator.Kind.SMALLER_THAN -> {
				val leftValue = left.getComputedValue() as? NumberLiteral ?: return
				val rightValue = right.getComputedValue() as? NumberLiteral ?: return
				BooleanLiteral(this, leftValue.value < rightValue.value)
			}
			Operator.Kind.GREATER_THAN -> {
				val leftValue = left.getComputedValue() as? NumberLiteral ?: return
				val rightValue = right.getComputedValue() as? NumberLiteral ?: return
				BooleanLiteral(this, leftValue.value > rightValue.value)
			}
			Operator.Kind.SMALLER_THAN_OR_EQUAL_TO -> {
				val leftValue = left.getComputedValue() as? NumberLiteral ?: return
				val rightValue = right.getComputedValue() as? NumberLiteral ?: return
				BooleanLiteral(this, leftValue.value <= rightValue.value)
			}
			Operator.Kind.GREATER_THAN_OR_EQUAL_TO -> {
				val leftValue = left.getComputedValue() as? NumberLiteral ?: return
				val rightValue = right.getComputedValue() as? NumberLiteral ?: return
				BooleanLiteral(this, leftValue.value >= rightValue.value)
			}
			Operator.Kind.IDENTICAL_TO -> {
				val leftValue = left.getComputedValue() ?: return
				val rightValue = right.getComputedValue() ?: return
				BooleanLiteral(this, leftValue == rightValue)
			}
			Operator.Kind.NOT_IDENTICAL_TO -> {
				val leftValue = left.getComputedValue() ?: return
				val rightValue = right.getComputedValue() ?: return
				BooleanLiteral(this, leftValue != rightValue)
			}
			Operator.Kind.EQUAL_TO -> {
				val leftValue = left.getComputedValue() as? LiteralValue ?: return
				val rightValue = right.getComputedValue() as? LiteralValue ?: return
				BooleanLiteral(this, leftValue == rightValue)
			}
			Operator.Kind.NOT_EQUAL_TO -> {
				val leftValue = left.getComputedValue() as? LiteralValue ?: return
				val rightValue = right.getComputedValue() as? LiteralValue ?: return
				BooleanLiteral(this, leftValue != rightValue)
			}
			else -> throw CompilerError(source, "Static evaluation is not implemented for operators of kind '$kind'.")
		}
	}

	override fun validate() {
		super.validate()
		validateWhereClauseConditions()
		validateMonomorphicAccess()
	}

	private fun validateWhereClauseConditions() {
		val signature = targetSignature ?: return
		val leftType = left.providedType ?: return
		val typeParameters = (leftType as? ObjectType)?.typeParameters ?: emptyList()
		for(condition in signature.whereClauseConditions) {
			if(!condition.isMet(typeParameters))
				context.addIssue(WhereClauseUnfulfilled(source, "Operator",
					signature.original.toString(false, kind), leftType, condition))
		}
	}

	private fun validateMonomorphicAccess() {
		val signature = targetSignature ?: return
		val leftType = left.providedType ?: return
		if(signature.associatedImplementation?.isAbstract == true && signature.associatedImplementation.isMonomorphic
			&& !leftType.isMemberAccessible(signature, true))
			context.addIssue(AbstractMonomorphicAccess(source, "operator",
				signature.toString(false, kind), leftType))
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		if(kind == Operator.Kind.DOUBLE_QUESTION_MARK)
			return getNullCoalescenceResult(constructor)
		val resultName = "_binaryOperatorResult"
		var leftValue = ValueConverter.convertIfRequired(this, constructor, left.getLlvmValue(constructor), left.effectiveType,
			left.hasGenericType, left.effectiveType, false)
		val rightType = targetSignature?.parameterTypes?.firstOrNull() ?: right.effectiveType
		val originalRightType = targetSignature?.original?.parameterTypes?.firstOrNull() ?: right.effectiveType
		var rightValue = ValueConverter.convertIfRequired(this, constructor, right.getLlvmValue(constructor), right.effectiveType,
			right.hasGenericType, rightType, rightType != originalRightType, conversions?.get(right))
		if(SpecialType.BOOLEAN.matches(left.effectiveType) && SpecialType.BOOLEAN.matches(right.effectiveType)) {
			when(kind) {
				Operator.Kind.AND -> return constructor.buildAnd(leftValue, rightValue, resultName)
				Operator.Kind.OR -> return constructor.buildOr(leftValue, rightValue, resultName)
				Operator.Kind.EQUAL_TO -> return constructor.buildBooleanEqualTo(leftValue, rightValue, resultName)
				Operator.Kind.NOT_EQUAL_TO -> return constructor.buildBooleanNotEqualTo(leftValue, rightValue, resultName)
				else -> {}
			}
		}
		val isLeftFloat = SpecialType.FLOAT.matches(left.effectiveType)
		val isLeftInteger = SpecialType.INTEGER.matches(left.effectiveType)
		val isLeftPrimitiveNumber = SpecialType.BYTE.matches(left.effectiveType) || isLeftInteger || isLeftFloat
		val isRightFloat = SpecialType.FLOAT.matches(right.effectiveType)
		val isRightInteger = SpecialType.INTEGER.matches(right.effectiveType)
		val isRightPrimitiveNumber = SpecialType.BYTE.matches(right.effectiveType) || isRightInteger || isRightFloat
		if(isLeftPrimitiveNumber && isRightPrimitiveNumber) {
			val intermediateOperandName = "_implicitlyCastBinaryOperand"
			val isFloatOperation = isLeftFloat || isRightFloat
			if(isFloatOperation) {
				if(!isLeftFloat)
					leftValue = constructor.buildCastFromSignedIntegerToFloat(leftValue, intermediateOperandName)
				else if(!isRightFloat)
					rightValue = constructor.buildCastFromSignedIntegerToFloat(rightValue, intermediateOperandName)
			} else {
				val isIntegerOperation = isLeftInteger || isRightInteger
				if(isIntegerOperation) {
					if(!isLeftInteger)
						leftValue = constructor.buildCastFromByteToInteger(leftValue, intermediateOperandName)
					else if(!isRightInteger)
						rightValue = constructor.buildCastFromByteToInteger(rightValue, intermediateOperandName)
				}
			}
			when(kind) {
				Operator.Kind.PLUS -> {
					return if(isFloatOperation)
						constructor.buildFloatAddition(leftValue, rightValue, resultName)
					else
						constructor.buildIntegerAddition(leftValue, rightValue, resultName)
				}
				Operator.Kind.MINUS -> {
					return if(isFloatOperation)
						constructor.buildFloatSubtraction(leftValue, rightValue, resultName)
					else
						constructor.buildIntegerSubtraction(leftValue, rightValue, resultName)
				}
				Operator.Kind.STAR -> {
					return if(isFloatOperation)
						constructor.buildFloatMultiplication(leftValue, rightValue, resultName)
					else
						constructor.buildIntegerMultiplication(leftValue, rightValue, resultName)
				}
				Operator.Kind.SLASH -> {
					return if(isFloatOperation)
						constructor.buildFloatDivision(leftValue, rightValue, resultName)
					else
						constructor.buildSignedIntegerDivision(leftValue, rightValue, resultName)
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
		if(kind == Operator.Kind.EQUAL_TO) {
			if(SpecialType.NULL.matches(left.effectiveType)) {
				return constructor.buildPointerEqualTo(constructor.nullPointer, rightValue, resultName)
			} else if(left.effectiveType is OptionalType) { //TODO check is effectiveType simplified?
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
		} else if(kind == Operator.Kind.NOT_EQUAL_TO) {
			if(SpecialType.NULL.matches(left.effectiveType)) {
				return constructor.buildPointerNotEqualTo(constructor.nullPointer, rightValue, resultName)
			} else if(left.effectiveType is OptionalType) {
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
		val signature = targetSignature?.original ?: throw CompilerError(source, "Binary operator is missing a target.")
		val resultName = "_binaryOperatorResult"
		val parameters = LinkedList<LlvmValue>()
		parameters.add(context.getExceptionParameter(constructor))
		parameters.add(leftValue)
		parameters.add(rightValue)
		val typeDefinition = signature.parentTypeDeclaration
		val returnValue = if(typeDefinition?.isLlvmPrimitive() == true) {
			val primitiveImplementation = context.nativeRegistry.resolvePrimitiveImplementation(
				"${typeDefinition.name}${signature.original.toString(false, kind)}")
			constructor.buildFunctionCall(primitiveImplementation.llvmType, primitiveImplementation.llvmValue, parameters, resultName)
		} else {
			val functionAddress = context.resolveFunction(constructor, leftValue, signature.getIdentifier(kind))
			constructor.buildFunctionCall(signature.getLlvmType(constructor), functionAddress, parameters, resultName)
		}
		context.continueRaise(constructor, this)
		return returnValue
	}

	private fun getNullCoalescenceResult(constructor: LlvmConstructor): LlvmValue {
		val rightValue = right.getLlvmValue(constructor)
		if(SpecialType.NULL.matches(left.providedType))
			return rightValue
		var leftValue = left.getLlvmValue(constructor)
		val leftType = left.providedType
		if(leftType !is OptionalType)
			return leftValue
		val resultType = providedType?.getLlvmType(constructor)
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
		if(providedType?.isLlvmPrimitive() == true && leftType.baseType.isLlvmPrimitive())
			leftValue = constructor.buildLoad(leftType.baseType.getLlvmType(constructor), leftValue, "_unboxedPrimitive")
		constructor.buildStore(leftValue, result)
		constructor.buildJump(resultBlock)
		constructor.select(resultBlock)
		return constructor.buildLoad(resultType, result, "_nullCoalescence_result")
	}
}
