package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.VariableUsage
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.NumberLiteral
import components.semantic_analysis.semantic_model.values.Operator
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.NotFound
import components.syntax_parser.syntax_tree.operations.BinaryModification as BinaryModificationSyntaxTree

class BinaryModification(override val source: BinaryModificationSyntaxTree, scope: Scope, val target: Value, val modifier: Value,
						 val kind: Operator.Kind): Unit(source, scope) {

	init {
		addUnits(target, modifier)
	}

	override fun linkValues(linter: Linter) {
		super.linkValues(linter)
		target.type?.let { valueType ->
			try {
				val operatorDefinition = valueType.interfaceScope.resolveOperator(linter, kind, listOf(modifier))
				if(operatorDefinition == null) {
					linter.addIssue(NotFound(source, "Operator", "$valueType $kind ${modifier.type}"))
				}
			} catch(error: SignatureResolutionAmbiguityError) {
				//TODO write test for this
				error.log(linter, source, "operator", "$valueType $kind ${modifier.type}")
			}
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		modifier.analyseDataFlow(tracker)
		if(target is VariableValue) {
			tracker.add(listOf(VariableUsage.Kind.READ, VariableUsage.Kind.MUTATION), target, tracker.getCurrentTypeOf(target.definition),
				getComputedTargetValue(tracker))
		} else {
			target.analyseDataFlow(tracker)
		}
	}

	private fun getComputedTargetValue(tracker: VariableTracker): Value? {
		val targetValue = (target.getComputedValue(tracker) as? NumberLiteral ?: return null).value
		val modifierValue = (modifier.getComputedValue(tracker) as? NumberLiteral ?: return null).value
		val resultingValue = when(kind) {
			Operator.Kind.PLUS_EQUALS -> targetValue + modifierValue
			Operator.Kind.MINUS_EQUALS -> targetValue - modifierValue
			Operator.Kind.STAR_EQUALS -> targetValue * modifierValue
			Operator.Kind.SLASH_EQUALS -> targetValue / modifierValue
			else -> throw CompilerError(source, "Static evaluation is not implemented for operators of kind '$kind'.")
		}
		return NumberLiteral(source, scope, resultingValue, tracker.linter)
	}
}
