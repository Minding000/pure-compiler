package components.semantic_analysis.semantic_model.operations

import errors.user.SignatureResolutionAmbiguityError
import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.OperatorDefinition
import components.semantic_analysis.semantic_model.values.Value
import messages.Message
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.BooleanLiteral
import components.semantic_analysis.semantic_model.values.NullLiteral
import components.semantic_analysis.semantic_model.values.NumberLiteral
import errors.internal.CompilerError
import components.syntax_parser.syntax_tree.operations.BinaryOperator as BinaryOperatorSyntaxTree

class BinaryOperator(override val source: BinaryOperatorSyntaxTree, val left: Value, val right: Value,
					 val kind: OperatorDefinition.Kind): Value(source) {

	init {
		addUnits(left, right)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		left.type?.let { leftType ->
			try {
				val operatorDefinition = leftType.scope.resolveOperator(kind, right)
				if(operatorDefinition == null) {
					linter.addMessage(source,
						"Operator '$leftType $kind ${right.type}' hasn't been declared yet.",
						Message.Type.ERROR)
					return@let
				}
				type = operatorDefinition.returnType
			} catch(error: SignatureResolutionAmbiguityError) {
				linter.addMessage(source,
					"Call to operator '$leftType $kind ${right.type}' is ambiguous. " +
					"Matching signatures:" + error.getSignatureList(), Message.Type.ERROR) //TODO write test for this
			}
		}
		staticValue = calculateStaticResult()
	}

	private fun calculateStaticResult(): Value? {
		return when(kind) {
			OperatorDefinition.Kind.DOUBLE_QUESTION_MARK -> {
				val leftValue = left.staticValue ?: return null
				if(leftValue is NullLiteral)
					right.staticValue
				else
					leftValue
			}
			OperatorDefinition.Kind.AND -> {
				val leftValue = left.staticValue as? BooleanLiteral ?: return null
				val rightValue = right.staticValue as? BooleanLiteral ?: return null
				BooleanLiteral(source, leftValue.value && rightValue.value)
			}
			OperatorDefinition.Kind.PIPE -> {
				val leftValue = left.staticValue as? BooleanLiteral ?: return null
				val rightValue = right.staticValue as? BooleanLiteral ?: return null
				BooleanLiteral(source, leftValue.value || rightValue.value)
			}
			OperatorDefinition.Kind.PLUS -> {
				val leftValue = left.staticValue as? NumberLiteral ?: return null
				val rightValue = right.staticValue as? NumberLiteral ?: return null
				NumberLiteral(source, leftValue.value + rightValue.value)
			}
			OperatorDefinition.Kind.MINUS -> {
				val leftValue = left.staticValue as? NumberLiteral ?: return null
				val rightValue = right.staticValue as? NumberLiteral ?: return null
				NumberLiteral(source, leftValue.value - rightValue.value)
			}
			OperatorDefinition.Kind.STAR -> {
				val leftValue = left.staticValue as? NumberLiteral ?: return null
				val rightValue = right.staticValue as? NumberLiteral ?: return null
				NumberLiteral(source, leftValue.value * rightValue.value)
			}
			OperatorDefinition.Kind.SLASH -> {
				val leftValue = left.staticValue as? NumberLiteral ?: return null
				val rightValue = right.staticValue as? NumberLiteral ?: return null
				NumberLiteral(source, leftValue.value / rightValue.value)
			}
			OperatorDefinition.Kind.SMALLER_THAN -> {
				val leftValue = left.staticValue as? NumberLiteral ?: return null
				val rightValue = right.staticValue as? NumberLiteral ?: return null
				BooleanLiteral(source, leftValue.value < rightValue.value)
			}
			OperatorDefinition.Kind.GREATER_THAN -> {
				val leftValue = left.staticValue as? NumberLiteral ?: return null
				val rightValue = right.staticValue as? NumberLiteral ?: return null
				BooleanLiteral(source, leftValue.value > rightValue.value)
			}
			OperatorDefinition.Kind.SMALLER_THAN_OR_EQUAL_TO -> {
				val leftValue = left.staticValue as? NumberLiteral ?: return null
				val rightValue = right.staticValue as? NumberLiteral ?: return null
				BooleanLiteral(source, leftValue.value <= rightValue.value)
			}
			OperatorDefinition.Kind.GREATER_THAN_OR_EQUAL_TO -> {
				val leftValue = left.staticValue as? NumberLiteral ?: return null
				val rightValue = right.staticValue as? NumberLiteral ?: return null
				BooleanLiteral(source, leftValue.value >= rightValue.value)
			}
			OperatorDefinition.Kind.EQUAL_TO -> {
				val leftValue = left.staticValue ?: return null
				val rightValue = right.staticValue ?: return null
				BooleanLiteral(source, leftValue == rightValue)
			}
			OperatorDefinition.Kind.NOT_EQUAL_TO -> {
				val leftValue = left.staticValue ?: return null
				val rightValue = right.staticValue ?: return null
				BooleanLiteral(source, leftValue != rightValue)
			}
			else -> throw CompilerError("Static evaluation is not implemented for operators of kind '$kind'.")
		}
	}
}
