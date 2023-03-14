package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
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
				val operatorDefinition = leftType.interfaceScope.resolveOperator(kind, right)
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
		staticValue = calculateStaticResult(linter)
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		left.analyseDataFlow(linter, tracker)
		right.analyseDataFlow(linter, tracker)
	}

	private fun calculateStaticResult(linter: Linter): Value? {
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
			else -> throw CompilerError("Static evaluation is not implemented for operators of kind '$kind'.")
		}
	}
}
