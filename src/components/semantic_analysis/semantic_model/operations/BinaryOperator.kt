package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.VariableUsage
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.*
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.NotFound
import components.syntax_parser.syntax_tree.operations.BinaryOperator as BinaryOperatorSyntaxTree

class BinaryOperator(override val source: BinaryOperatorSyntaxTree, scope: Scope, val left: Value, val right: Value,
					 val kind: Operator.Kind): Value(source, scope) {

	init {
		addUnits(left, right)
	}

	override fun linkValues(linter: Linter) {
		super.linkValues(linter)
		left.type?.let { leftType ->
			try {
				val operatorDefinition = leftType.interfaceScope.resolveOperator(linter, kind, right)
				if(operatorDefinition == null) {
					linter.addIssue(NotFound(source, "Operator", "$leftType $kind ${right.type}"))
					return@let
				}
				type = operatorDefinition.returnType
			} catch(error: SignatureResolutionAmbiguityError) {
				//TODO write test for this
				error.log(linter, source, "operator", "$leftType $kind ${right.type}")
			}
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		val isConditional = Linter.SpecialType.BOOLEAN.matches(left.type) && (kind == Operator.Kind.AND || kind == Operator.Kind.PIPE)
		val isComparison = kind == Operator.Kind.EQUAL_TO || kind == Operator.Kind.NOT_EQUAL_TO
		left.analyseDataFlow(tracker)
		if(isConditional) {
			val isAnd = kind == Operator.Kind.AND
			tracker.setVariableStates(left.getEndState(isAnd))
			val variableValue = left as? VariableValue
			val declaration = variableValue?.definition
			if(declaration != null) {
				val booleanLiteral = BooleanLiteral(source, scope, isAnd, tracker.linter)
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
		staticValue = getComputedValue(tracker)
	}

	override fun getComputedValue(tracker: VariableTracker): Value? {
		val linter = tracker.linter
		return when(kind) {
			Operator.Kind.DOUBLE_QUESTION_MARK -> {
				val leftValue = left.staticValue ?: return null
				if(leftValue is NullLiteral)
					right.staticValue
				else
					leftValue
			}
			Operator.Kind.AND -> {
				val leftValue = left.staticValue as? BooleanLiteral ?: return null
				val rightValue = right.staticValue as? BooleanLiteral ?: return null
				BooleanLiteral(source, scope, leftValue.value && rightValue.value, linter)
			}
			Operator.Kind.PIPE -> {
				val leftValue = left.staticValue as? BooleanLiteral ?: return null
				val rightValue = right.staticValue as? BooleanLiteral ?: return null
				BooleanLiteral(source, scope, leftValue.value || rightValue.value, linter)
			}
			Operator.Kind.PLUS -> {
				val leftValue = left.staticValue as? NumberLiteral ?: return null
				val rightValue = right.staticValue as? NumberLiteral ?: return null
				NumberLiteral(source, scope, leftValue.value + rightValue.value, linter)
			}
			Operator.Kind.MINUS -> {
				val leftValue = left.staticValue as? NumberLiteral ?: return null
				val rightValue = right.staticValue as? NumberLiteral ?: return null
				NumberLiteral(source, scope, leftValue.value - rightValue.value, linter)
			}
			Operator.Kind.STAR -> {
				val leftValue = left.staticValue as? NumberLiteral ?: return null
				val rightValue = right.staticValue as? NumberLiteral ?: return null
				NumberLiteral(source, scope, leftValue.value * rightValue.value, linter)
			}
			Operator.Kind.SLASH -> {
				val leftValue = left.staticValue as? NumberLiteral ?: return null
				val rightValue = right.staticValue as? NumberLiteral ?: return null
				NumberLiteral(source, scope, leftValue.value / rightValue.value, linter)
			}
			Operator.Kind.SMALLER_THAN -> {
				val leftValue = left.staticValue as? NumberLiteral ?: return null
				val rightValue = right.staticValue as? NumberLiteral ?: return null
				BooleanLiteral(source, scope, leftValue.value < rightValue.value, linter)
			}
			Operator.Kind.GREATER_THAN -> {
				val leftValue = left.staticValue as? NumberLiteral ?: return null
				val rightValue = right.staticValue as? NumberLiteral ?: return null
				BooleanLiteral(source, scope, leftValue.value > rightValue.value, linter)
			}
			Operator.Kind.SMALLER_THAN_OR_EQUAL_TO -> {
				val leftValue = left.staticValue as? NumberLiteral ?: return null
				val rightValue = right.staticValue as? NumberLiteral ?: return null
				BooleanLiteral(source, scope, leftValue.value <= rightValue.value, linter)
			}
			Operator.Kind.GREATER_THAN_OR_EQUAL_TO -> {
				val leftValue = left.staticValue as? NumberLiteral ?: return null
				val rightValue = right.staticValue as? NumberLiteral ?: return null
				BooleanLiteral(source, scope, leftValue.value >= rightValue.value, linter)
			}
			Operator.Kind.EQUAL_TO -> {
				val leftValue = left.staticValue ?: return null
				val rightValue = right.staticValue ?: return null
				BooleanLiteral(source, scope, leftValue == rightValue, linter)
			}
			Operator.Kind.NOT_EQUAL_TO -> {
				val leftValue = left.staticValue ?: return null
				val rightValue = right.staticValue ?: return null
				BooleanLiteral(source, scope, leftValue != rightValue, linter)
			}
			else -> throw CompilerError(source, "Static evaluation is not implemented for operators of kind '$kind'.")
		}
	}
}
