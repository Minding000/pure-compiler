package components.semantic_model.operations

import components.code_generation.llvm.models.operations.UnaryModification
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.types.ObjectType
import components.semantic_model.values.NumberLiteral
import components.semantic_model.values.Operator
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.access.WhereClauseUnfulfilled
import logger.issues.resolution.NotFound
import java.math.BigDecimal
import components.syntax_parser.syntax_tree.operations.UnaryModification as UnaryModificationSyntaxTree

class UnaryModification(override val source: UnaryModificationSyntaxTree, scope: Scope, val target: Value, val kind: Operator.Kind):
	SemanticModel(source, scope) {
	var targetSignature: FunctionSignature? = null

	companion object {
		val STEP_SIZE = BigDecimal(1)
	}

	init {
		addSemanticModels(target)
	}

	override fun determineTypes() {
		super.determineTypes()
		context.registerWrite(target)
		val targetType = target.effectiveType ?: return
		try {
			val match = targetType.interfaceScope.getOperator(kind)
			if(match == null) {
				context.addIssue(NotFound(source, "Operator", "$targetType$kind"))
				return
			}
			targetSignature = match.signature
		} catch(error: SignatureResolutionAmbiguityError) {
			//TODO write test for this
			error.log(source, "operator", "$targetType$kind")
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		if(target is VariableValue) {
			target.computeValue(tracker)
			tracker.add(listOf(VariableUsage.Kind.READ, VariableUsage.Kind.MUTATION), target, tracker.getCurrentTypeOf(target.declaration),
				getComputedTargetValue())
		} else {
			target.analyseDataFlow(tracker)
		}
	}

	private fun getComputedTargetValue(): Value? {
		val targetValue = (target.getComputedValue() as? NumberLiteral ?: return null).value
		val resultingValue = when(kind) {
			Operator.Kind.DOUBLE_PLUS -> targetValue + STEP_SIZE
			Operator.Kind.DOUBLE_MINUS -> targetValue - STEP_SIZE
			else -> throw CompilerError(source, "Static evaluation is not implemented for operators of kind '$kind'.")
		}
		return NumberLiteral(this, resultingValue)
	}

	override fun validate() {
		super.validate()
		validateWhereClauseConditions()
	}

	private fun validateWhereClauseConditions() {
		val signature = targetSignature ?: return
		val targetType = target.providedType ?: return
		val typeParameters = (targetType as? ObjectType)?.typeParameters ?: emptyList()
		for(condition in signature.whereClauseConditions) {
			if(!condition.isMet(typeParameters))
				context.addIssue(WhereClauseUnfulfilled(source, "Operator",
					signature.original.toString(false, kind), targetType, condition))
		}
	}

	override fun toUnit() = UnaryModification(this, target.toUnit())
}
