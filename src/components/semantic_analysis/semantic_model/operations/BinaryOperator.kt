package components.semantic_analysis.semantic_model.operations

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.context.VariableUsage
import components.semantic_analysis.semantic_model.definitions.FunctionSignature
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.*
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.NotFound
import components.syntax_parser.syntax_tree.operations.BinaryOperator as BinaryOperatorSyntaxTree

class BinaryOperator(override val source: BinaryOperatorSyntaxTree, scope: Scope, val left: Value, val right: Value,
					 val kind: Operator.Kind): Value(source, scope) {
	var targetSignature: FunctionSignature? = null

	init {
		addSemanticModels(left, right)
	}

	override fun determineTypes() {
		super.determineTypes()
		left.type?.let { leftType ->
			try {
				targetSignature = leftType.interfaceScope.resolveOperator(kind, right)
				if(targetSignature == null) {
					context.addIssue(NotFound(source, "Operator", "$leftType $kind ${right.type}"))
					return@let
				}
				type = targetSignature?.returnType
			} catch(error: SignatureResolutionAmbiguityError) {
				//TODO write test for this
				error.log(source, "operator", "$leftType $kind ${right.type}")
			}
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		val isConditional = SpecialType.BOOLEAN.matches(left.type) && (kind == Operator.Kind.AND || kind == Operator.Kind.PIPE)
		val isComparison = kind == Operator.Kind.EQUAL_TO || kind == Operator.Kind.NOT_EQUAL_TO
		left.analyseDataFlow(tracker)
		if(isConditional) {
			val isAnd = kind == Operator.Kind.AND
			tracker.setVariableStates(left.getEndState(isAnd))
			val variableValue = left as? VariableValue
			val declaration = variableValue?.definition
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
			val declaration = variableValue?.definition
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
			Operator.Kind.PIPE -> {
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
			Operator.Kind.EQUAL_TO -> {
				val leftValue = left.getComputedValue() ?: return
				val rightValue = right.getComputedValue() ?: return
				val areValuesEqual = leftValue == rightValue
				val isIdentityComparison = leftValue !is LiteralValue || rightValue !is LiteralValue
				if(!areValuesEqual && isIdentityComparison)
					return
				BooleanLiteral(this, areValuesEqual)
			}
			Operator.Kind.NOT_EQUAL_TO -> {
				val leftValue = left.getComputedValue() ?: return
				val rightValue = right.getComputedValue() ?: return
				val areValuesEqual = leftValue == rightValue
				val isIdentityComparison = leftValue !is LiteralValue || rightValue !is LiteralValue
				if(!areValuesEqual && isIdentityComparison)
					return
				BooleanLiteral(this, !areValuesEqual)
			}
			else -> throw CompilerError(source, "Static evaluation is not implemented for operators of kind '$kind'.")
		}
	}

	override fun createLlvmValue(constructor: LlvmConstructor): LlvmValue {
		var leftValue = left.getLlvmValue(constructor)
		var rightValue = right.getLlvmValue(constructor)
		if(SpecialType.BOOLEAN.matches(left.type) && SpecialType.BOOLEAN.matches(right.type)) {
			when(kind) {
				Operator.Kind.AND -> return constructor.buildAnd(leftValue, rightValue, "and")
				Operator.Kind.PIPE -> return constructor.buildOr(leftValue, rightValue, "or")
				Operator.Kind.EQUAL_TO -> return constructor.buildBooleanEqualTo(leftValue, rightValue, "boolean equal_to")
				Operator.Kind.NOT_EQUAL_TO -> return constructor.buildBooleanNotEqualTo(leftValue, rightValue, "boolean not_equal_to")
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
				if(isLeftInteger)
					leftValue = constructor.buildCastFromSignedIntegerToFloat(leftValue, "cast operand to match operation")
				else if(isRightInteger)
					rightValue = constructor.buildCastFromSignedIntegerToFloat(rightValue, "cast operand to match operation")
			}
			when(kind) {
				Operator.Kind.PLUS -> {
					return if(isIntegerOperation)
						constructor.buildIntegerAddition(leftValue, rightValue, "integer addition")
					else
						constructor.buildFloatAddition(leftValue, rightValue, "float addition")
				}
				Operator.Kind.MINUS -> {
					return if(isIntegerOperation)
						constructor.buildIntegerSubtraction(leftValue, rightValue, "integer subtraction")
					else
						constructor.buildFloatSubtraction(leftValue, rightValue, "float subtraction")
				}
				Operator.Kind.STAR -> {
					return if(isIntegerOperation)
						constructor.buildIntegerMultiplication(leftValue, rightValue, "integer multiplication")
					else
						constructor.buildFloatMultiplication(leftValue, rightValue, "float multiplication")
				}
				Operator.Kind.SLASH -> {
					return if(isIntegerOperation)
						constructor.buildSignedIntegerDivision(leftValue, rightValue, "integer division")
					else
						constructor.buildFloatDivision(leftValue, rightValue, "float division")
				}
				Operator.Kind.SMALLER_THAN -> {
					return if(isIntegerOperation)
						constructor.buildSignedIntegerLessThan(leftValue, rightValue, "integer smaller_than")
					else
						constructor.buildFloatLessThan(leftValue, rightValue, "float smaller_than")
				}
				Operator.Kind.GREATER_THAN -> {
					return if(isIntegerOperation)
						constructor.buildSignedIntegerGreaterThan(leftValue, rightValue, "integer greater_than")
					else
						constructor.buildFloatGreaterThan(leftValue, rightValue, "float greater_than")
				}
				Operator.Kind.SMALLER_THAN_OR_EQUAL_TO -> {
					return if(isIntegerOperation)
						constructor.buildSignedIntegerLessThanOrEqualTo(leftValue, rightValue, "integer smaller_than_or_equal_to")
					else
						constructor.buildFloatLessThanOrEqualTo(leftValue, rightValue, "float smaller_than_or_equal_to")
				}
				Operator.Kind.GREATER_THAN_OR_EQUAL_TO -> {
					return if(isIntegerOperation)
						constructor.buildSignedIntegerGreaterThanOrEqualTo(leftValue, rightValue, "integer greater_than_or_equal_to")
					else
						constructor.buildFloatGreaterThanOrEqualTo(leftValue, rightValue, "float greater_than_or_equal_to")
				}
				Operator.Kind.EQUAL_TO -> {
					return if(isIntegerOperation)
						constructor.buildSignedIntegerEqualTo(leftValue, rightValue, "integer equal_to")
					else
						constructor.buildFloatEqualTo(leftValue, rightValue, "float equal_to")
				}
				Operator.Kind.NOT_EQUAL_TO -> {
					return if(isIntegerOperation)
						constructor.buildSignedIntegerNotEqualTo(leftValue, rightValue, "integer not_equal_to")
					else
						constructor.buildFloatNotEqualTo(leftValue, rightValue, "float not_equal_to")
				}
				else -> {}
			}
		}
		TODO("Binary '${left.type} $kind ${right.type}' operator is not implemented yet.")
	}
}
