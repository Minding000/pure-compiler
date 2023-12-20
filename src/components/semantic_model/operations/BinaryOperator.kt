package components.semantic_model.operations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.scopes.Scope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.OptionalType
import components.semantic_model.types.OrUnionType
import components.semantic_model.values.*
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.access.AbstractMonomorphicAccess
import logger.issues.access.WhereClauseUnfulfilled
import logger.issues.resolution.NotFound
import java.util.*
import components.syntax_parser.syntax_tree.operations.BinaryOperator as BinaryOperatorSyntaxTree

class BinaryOperator(override val source: BinaryOperatorSyntaxTree, scope: Scope, val left: Value, val right: Value,
					 val kind: Operator.Kind): Value(source, scope) {
	var targetSignature: FunctionSignature? = null

	init {
		addSemanticModels(left, right)
	}

	override fun determineTypes() {
		super.determineTypes()
		var leftType = left.type ?: return
		val rightType = right.type ?: return
		if(kind == Operator.Kind.DOUBLE_QUESTION_MARK) {
			if(SpecialType.NULL.matches(leftType)) {
				type = rightType
			} else {
				val nonOptionalLeftType = if(leftType is OptionalType) leftType.baseType else leftType
				type = OrUnionType(source, scope, listOf(nonOptionalLeftType, rightType)).simplified()
				addSemanticModels(type)
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
				context.addIssue(NotFound(source, "Operator", "$leftType $kind ${right.type}"))
				return
			}
			targetSignature = match.signature
			setUnextendedType(match.returnType.getLocalType(this, leftType))
			if(match.signature.associatedImplementation?.isAbstract == true && match.signature.associatedImplementation.isMonomorphic
				&& !leftType.isMemberAccessible(match.signature, true))
				context.addIssue(AbstractMonomorphicAccess(source, "operator",
					match.signature.toString(false, kind), leftType))
		} catch(error: SignatureResolutionAmbiguityError) {
			//TODO write test for this
			error.log(source, "operator", "$leftType $kind ${right.type}")
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		val isConditional = SpecialType.BOOLEAN.matches(left.type) && (kind == Operator.Kind.AND || kind == Operator.Kind.OR)
		val isComparison = kind == Operator.Kind.EQUAL_TO || kind == Operator.Kind.NOT_EQUAL_TO
		left.analyseDataFlow(tracker)
		if(isConditional) {
			val isAnd = kind == Operator.Kind.AND
			tracker.setVariableStates(left.getEndState(isAnd))
			val variableValue = left as? VariableValue
			val declaration = variableValue?.declaration
			if(declaration != null) {
				val booleanLiteral = BooleanLiteral(this, isAnd)
				tracker.add(VariableUsage.Kind.HINT, declaration, this, booleanLiteral.type, booleanLiteral)
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
				tracker.add(VariableUsage.Kind.HINT, declaration, this, literalValue.type, literalValue)
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
	}

	private fun validateWhereClauseConditions() {
		val signature = targetSignature ?: return
		val leftType = left.type ?: return
		val typeParameters = (leftType as? ObjectType)?.typeParameters ?: emptyList()
		for(condition in signature.whereClauseConditions) {
			if(!condition.isMet(typeParameters))
				context.addIssue(WhereClauseUnfulfilled(source, "Operator",
					signature.original.toString(false, kind), leftType, condition))
		}
	}

	override fun createLlvmValue(constructor: LlvmConstructor): LlvmValue {
		if(kind == Operator.Kind.DOUBLE_QUESTION_MARK)
			return getNullCoalescenceResult(constructor)
		val resultName = "_binaryOperatorResult"
		var leftValue = left.getLlvmValue(constructor)
		var rightValue = right.getLlvmValue(constructor)
		if(SpecialType.BOOLEAN.matches(left.type) && SpecialType.BOOLEAN.matches(right.type)) {
			when(kind) {
				Operator.Kind.AND -> return constructor.buildAnd(leftValue, rightValue, resultName)
				Operator.Kind.OR -> return constructor.buildOr(leftValue, rightValue, resultName)
				Operator.Kind.EQUAL_TO -> return constructor.buildBooleanEqualTo(leftValue, rightValue, resultName)
				Operator.Kind.NOT_EQUAL_TO -> return constructor.buildBooleanNotEqualTo(leftValue, rightValue, resultName)
				else -> {}
			}
		}
		val isLeftInteger = SpecialType.INTEGER.matches(left.type)
		val isLeftPrimitiveNumber = isLeftInteger || SpecialType.FLOAT.matches(left.type)
		val isRightInteger = SpecialType.INTEGER.matches(right.type)
		val isRightPrimitiveNumber = isRightInteger || SpecialType.FLOAT.matches(right.type)
		if(isLeftPrimitiveNumber && isRightPrimitiveNumber) {
			val isIntegerOperation = isLeftInteger && isRightInteger
			if(!isIntegerOperation) {
				val intermediateOperandName = "_implicitlyCastBinaryOperand"
				if(isLeftInteger)
					leftValue = constructor.buildCastFromSignedIntegerToFloat(leftValue, intermediateOperandName)
				else if(isRightInteger)
					rightValue = constructor.buildCastFromSignedIntegerToFloat(rightValue, intermediateOperandName)
			}
			when(kind) {
				Operator.Kind.PLUS -> {
					return if(isIntegerOperation)
						constructor.buildIntegerAddition(leftValue, rightValue, resultName)
					else
						constructor.buildFloatAddition(leftValue, rightValue, resultName)
				}
				Operator.Kind.MINUS -> {
					return if(isIntegerOperation)
						constructor.buildIntegerSubtraction(leftValue, rightValue, resultName)
					else
						constructor.buildFloatSubtraction(leftValue, rightValue, resultName)
				}
				Operator.Kind.STAR -> {
					return if(isIntegerOperation)
						constructor.buildIntegerMultiplication(leftValue, rightValue, resultName)
					else
						constructor.buildFloatMultiplication(leftValue, rightValue, resultName)
				}
				Operator.Kind.SLASH -> {
					return if(isIntegerOperation)
						constructor.buildSignedIntegerDivision(leftValue, rightValue, resultName)
					else
						constructor.buildFloatDivision(leftValue, rightValue, resultName)
				}
				Operator.Kind.SMALLER_THAN -> {
					return if(isIntegerOperation)
						constructor.buildSignedIntegerLessThan(leftValue, rightValue, resultName)
					else
						constructor.buildFloatLessThan(leftValue, rightValue, resultName)
				}
				Operator.Kind.GREATER_THAN -> {
					return if(isIntegerOperation)
						constructor.buildSignedIntegerGreaterThan(leftValue, rightValue, resultName)
					else
						constructor.buildFloatGreaterThan(leftValue, rightValue, resultName)
				}
				Operator.Kind.SMALLER_THAN_OR_EQUAL_TO -> {
					return if(isIntegerOperation)
						constructor.buildSignedIntegerLessThanOrEqualTo(leftValue, rightValue, resultName)
					else
						constructor.buildFloatLessThanOrEqualTo(leftValue, rightValue, resultName)
				}
				Operator.Kind.GREATER_THAN_OR_EQUAL_TO -> {
					return if(isIntegerOperation)
						constructor.buildSignedIntegerGreaterThanOrEqualTo(leftValue, rightValue, resultName)
					else
						constructor.buildFloatGreaterThanOrEqualTo(leftValue, rightValue, resultName)
				}
				Operator.Kind.EQUAL_TO -> {
					return if(isIntegerOperation)
						constructor.buildSignedIntegerEqualTo(leftValue, rightValue, resultName)
					else
						constructor.buildFloatEqualTo(leftValue, rightValue, resultName)
				}
				Operator.Kind.NOT_EQUAL_TO -> {
					return if(isIntegerOperation)
						constructor.buildSignedIntegerNotEqualTo(leftValue, rightValue, resultName)
					else
						constructor.buildFloatNotEqualTo(leftValue, rightValue, resultName)
				}
				else -> {}
			}
		}
		val signature = targetSignature?.original ?: throw CompilerError(source, "Binary operator is missing a target.")
		return createLlvmFunctionCall(constructor, signature)
	}

	private fun createLlvmFunctionCall(constructor: LlvmConstructor, signature: FunctionSignature): LlvmValue {
		val typeDefinition = signature.parentDefinition
		val targetValue = left.getLlvmValue(constructor)
		val parameters = LinkedList<LlvmValue>()
		val exceptionAddress = constructor.buildStackAllocation(constructor.pointerType, "__exceptionAddress")
		parameters.add(exceptionAddress)
		parameters.add(targetValue)
		parameters.add(right.getLlvmValue(constructor))
		val functionAddress = context.resolveFunction(constructor, typeDefinition?.llvmType, targetValue,
			signature.original.toString(false, kind))
		return constructor.buildFunctionCall(signature.getLlvmType(constructor), functionAddress, parameters, "_binaryOperatorResult")
		//TODO if exception exists
		// check for optional try (normal and force try have no effect)
		// check for catch
		// resume raise
	}

	private fun getNullCoalescenceResult(constructor: LlvmConstructor): LlvmValue {
		val rightValue = right.getLlvmValue(constructor)
		if(SpecialType.NULL.matches(left.type))
			return rightValue
		var leftValue = left.getLlvmValue(constructor)
		val leftType = left.type
		if(leftType !is OptionalType)
			return leftValue
		val resultType = type?.getLlvmType(constructor)
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
		if(type?.isLlvmPrimitive() == true && leftType.baseType.isLlvmPrimitive())
			leftValue = constructor.buildLoad(leftType.baseType.getLlvmType(constructor), leftValue, "_unboxedPrimitive")
		constructor.buildStore(leftValue, result)
		constructor.buildJump(resultBlock)
		constructor.select(resultBlock)
		return constructor.buildLoad(resultType, result, "_nullCoalescence_result")
	}
}
