package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.VariableUsage
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.*
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.NotFound
import java.math.BigDecimal
import components.syntax_parser.syntax_tree.operations.UnaryModification as UnaryModificationSyntaxTree

class UnaryModification(override val source: UnaryModificationSyntaxTree, scope: Scope, val target: Value, val kind: Operator.Kind):
	Unit(source, scope) {

	companion object {
		private val STEP = BigDecimal(1)
	}

	init {
		addUnits(target)
	}

	override fun linkValues(linter: Linter) {
		super.linkValues(linter)
		target.type?.let { valueType ->
			try {
				val operatorDefinition = valueType.interfaceScope.resolveOperator(linter, kind)
				if(operatorDefinition == null)
					linter.addIssue(NotFound(source, "Operator", "$valueType$kind"))
			} catch(error: SignatureResolutionAmbiguityError) {
				//TODO write test for this
				error.log(linter, source, "operator", "$valueType$kind")
			}
		}
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		if(target is VariableValue) {
			tracker.add(listOf(VariableUsage.Kind.READ, VariableUsage.Kind.MUTATION), target, tracker.getCurrentTypeOf(target.definition),
				getComputedLiteralValue(linter, tracker))
		} else {
			target.analyseDataFlow(linter, tracker)
		}
	}

	private fun getComputedLiteralValue(linter: Linter, tracker: VariableTracker): LiteralValue? {
		val targetValue = (target.getComputedLiteralValue(tracker) as? NumberLiteral ?: return null).value
		val resultingValue = when(kind) {
			Operator.Kind.DOUBLE_PLUS -> targetValue + STEP
			Operator.Kind.DOUBLE_MINUS -> targetValue - STEP
			else -> throw CompilerError(source, "Static evaluation is not implemented for operators of kind '$kind'.")
		}
		return NumberLiteral(source, scope, resultingValue, linter)
	}
}
