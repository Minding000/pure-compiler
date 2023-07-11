package components.semantic_analysis.semantic_model.operations

import components.compiler.targets.llvm.LlvmConstructor
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.context.VariableUsage
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.NumberLiteral
import components.semantic_analysis.semantic_model.values.Operator
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.NotFound
import java.math.BigDecimal
import components.syntax_parser.syntax_tree.operations.UnaryModification as UnaryModificationSyntaxTree

class UnaryModification(override val source: UnaryModificationSyntaxTree, scope: Scope, val target: Value, val kind: Operator.Kind):
	SemanticModel(source, scope) {

	companion object {
		private val STEP = BigDecimal(1)
	}

	init {
		addSemanticModels(target)
	}

	override fun determineTypes() {
		super.determineTypes()
		context.registerWrite(target)
		target.type?.let { targetType ->
			try {
				val operatorDefinition = targetType.interfaceScope.resolveOperator(kind)
				if(operatorDefinition == null)
					context.addIssue(NotFound(source, "Operator", "$targetType$kind"))
			} catch(error: SignatureResolutionAmbiguityError) {
				//TODO write test for this
				error.log(source, "operator", "$targetType$kind")
			}
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		if(target is VariableValue) {
			target.computeValue(tracker)
			tracker.add(listOf(VariableUsage.Kind.READ, VariableUsage.Kind.MUTATION), target, tracker.getCurrentTypeOf(target.definition),
				getComputedTargetValue())
		} else {
			target.analyseDataFlow(tracker)
		}
	}

	private fun getComputedTargetValue(): Value? {
		val targetValue = (target.getComputedValue() as? NumberLiteral ?: return null).value
		val resultingValue = when(kind) {
			Operator.Kind.DOUBLE_PLUS -> targetValue + STEP
			Operator.Kind.DOUBLE_MINUS -> targetValue - STEP
			else -> throw CompilerError(source, "Static evaluation is not implemented for operators of kind '$kind'.")
		}
		return NumberLiteral(this, resultingValue)
	}

	override fun compile(constructor: LlvmConstructor) {
		super.compile(constructor)
		if(target is VariableValue) {
			if(SpecialType.INTEGER.matches(target.type)) {
				val targetValue = target.getLlvmValue(constructor)
				val modifierValue = constructor.buildInt32(1)
				val operation = when(kind) {
					Operator.Kind.DOUBLE_PLUS -> constructor.buildIntegerAddition(targetValue, modifierValue, "increment")
					Operator.Kind.DOUBLE_MINUS -> constructor.buildIntegerSubtraction(targetValue, modifierValue, "decrement")
					else -> throw CompilerError(source, "Unknown native unary integer modification of kind '$kind'.")
				}
				constructor.buildStore(operation, target.definition?.llvmLocation)
			}
		}
	}
}
