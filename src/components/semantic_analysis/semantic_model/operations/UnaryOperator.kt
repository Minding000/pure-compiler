package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.BooleanLiteral
import components.semantic_analysis.semantic_model.values.NumberLiteral
import components.semantic_analysis.semantic_model.values.Operator
import components.semantic_analysis.semantic_model.values.Value
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.NotFound
import components.syntax_parser.syntax_tree.operations.UnaryOperator as UnaryOperatorSyntaxTree

class UnaryOperator(override val source: UnaryOperatorSyntaxTree, scope: Scope, val value: Value, val kind: Operator.Kind):
	Value(source, scope) {

	init {
		addUnits(value)
	}

	override fun linkValues(linter: Linter) {
		super.linkValues(linter)
		value.type?.let { valueType ->
			try {
				val operatorDefinition = valueType.interfaceScope.resolveOperator(linter, kind)
				if(operatorDefinition == null) {
					linter.addIssue(NotFound(source, "Operator", "$kind$valueType"))
					return@let
				}
				type = operatorDefinition.returnType
			} catch(error: SignatureResolutionAmbiguityError) {
				//TODO write test for this
				error.log(linter, source, "operator", "$kind$valueType")
			}
		}
		staticValue = calculateStaticResult(linter)
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		super.analyseDataFlow(linter, tracker)
		if(Linter.SpecialType.BOOLEAN.matches(value.type) && kind == Operator.Kind.EXCLAMATION_MARK) {
			positiveState = value.getNegativeEndState()
			negativeState = value.getPositiveEndState()
		}
	}

	private fun calculateStaticResult(linter: Linter): Value? {
		return when(kind) {
			Operator.Kind.BRACKETS_GET -> null
			Operator.Kind.EXCLAMATION_MARK -> {
				val booleanValue = value.staticValue as? BooleanLiteral ?: return null
				BooleanLiteral(source, scope, !booleanValue.value, linter)
			}
			Operator.Kind.TRIPLE_DOT -> null
			Operator.Kind.MINUS -> {
				val numberValue = value.staticValue as? NumberLiteral ?: return null
				NumberLiteral(source, scope, -numberValue.value, linter)
			}
			else -> throw CompilerError(source, "Static evaluation is not implemented for operators of kind '$kind'.")
		}
	}
}
