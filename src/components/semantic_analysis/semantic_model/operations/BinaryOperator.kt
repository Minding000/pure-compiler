package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.context.VariableUsage
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.*
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.NotFound
import components.syntax_parser.syntax_tree.operations.BinaryOperator as BinaryOperatorSyntaxTree

class BinaryOperator(override val source: BinaryOperatorSyntaxTree, scope: Scope, val left: Value, val right: Value,
					 val kind: Operator.Kind): Value(source, scope) {

	init {
		addSemanticModels(left, right)
	}

	override fun determineTypes() {
		super.determineTypes()
		left.type?.let { leftType ->
			try {
				val operatorDefinition = leftType.interfaceScope.resolveOperator(kind, right)
				if(operatorDefinition == null) {
					context.addIssue(NotFound(source, "Operator", "$leftType $kind ${right.type}"))
					return@let
				}
				type = operatorDefinition.returnType
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
				BooleanLiteral(this, leftValue == rightValue)
			}
			Operator.Kind.NOT_EQUAL_TO -> {
				val leftValue = left.getComputedValue() ?: return
				val rightValue = right.getComputedValue() ?: return
				BooleanLiteral(this, leftValue != rightValue)
			}
			else -> throw CompilerError(source, "Static evaluation is not implemented for operators of kind '$kind'.")
		}
	}
}
