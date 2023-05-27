package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.context.VariableTracker
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
		addSemanticModels(value)
	}

	override fun determineTypes() {
		super.determineTypes()
		value.type?.let { valueType ->
			try {
				val operatorDefinition = valueType.interfaceScope.resolveOperator(kind)
				if(operatorDefinition == null) {
					context.addIssue(NotFound(source, "Operator", "$kind$valueType"))
					return@let
				}
				type = operatorDefinition.returnType
			} catch(error: SignatureResolutionAmbiguityError) {
				//TODO write test for this
				error.log(source, "operator", "$kind$valueType")
			}
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		if(SpecialType.BOOLEAN.matches(value.type) && kind == Operator.Kind.EXCLAMATION_MARK) {
			positiveState = value.getNegativeEndState()
			negativeState = value.getPositiveEndState()
		}
		staticValue = when(kind) {
			Operator.Kind.BRACKETS_GET -> null
			Operator.Kind.EXCLAMATION_MARK -> {
				val booleanValue = value.getComputedValue() as? BooleanLiteral ?: return
				BooleanLiteral(this, !booleanValue.value)
			}
			Operator.Kind.TRIPLE_DOT -> null
			Operator.Kind.MINUS -> {
				val numberValue = value.getComputedValue() as? NumberLiteral ?: return
				NumberLiteral(this, -numberValue.value)
			}
			else -> throw CompilerError(source, "Static evaluation is not implemented for operators of kind '$kind'.")
		}
	}
}
