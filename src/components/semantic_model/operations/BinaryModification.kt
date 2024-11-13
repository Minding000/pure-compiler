package components.semantic_model.operations

import components.code_generation.llvm.models.operations.BinaryModification
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.types.ObjectType
import components.semantic_model.values.NumberLiteral
import components.semantic_model.values.Operator
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.access.AbstractMonomorphicAccess
import logger.issues.access.WhereClauseUnfulfilled
import logger.issues.resolution.NotFound
import components.syntax_parser.syntax_tree.operations.BinaryModification as BinaryModificationSyntaxTree

class BinaryModification(override val source: BinaryModificationSyntaxTree, scope: Scope, val target: Value, val modifier: Value,
						 val kind: Operator.Kind): SemanticModel(source, scope) {
	var targetSignature: FunctionSignature? = null
	var conversions: Map<Value, InitializerDefinition>? = null

	init {
		addSemanticModels(target, modifier)
	}

	override fun determineTypes() {
		super.determineTypes()
		context.registerWrite(target)
		val targetType = target.effectiveType ?: return
		val modifierType = modifier.effectiveType ?: return
		try {
			val match = targetType.interfaceScope.getOperator(kind, modifier)
			if(match == null) {
				context.addIssue(NotFound(source, "Operator", "$targetType $kind $modifierType"))
				return
			}
			targetSignature = match.signature
			conversions = match.conversions
		} catch(error: SignatureResolutionAmbiguityError) {
			//TODO write test for this
			error.log(source, "operator", "$targetType $kind $modifierType")
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		modifier.analyseDataFlow(tracker)
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
		val modifierValue = (modifier.getComputedValue() as? NumberLiteral ?: return null).value
		val resultingValue = when(kind) {
			Operator.Kind.PLUS_EQUALS -> targetValue + modifierValue
			Operator.Kind.MINUS_EQUALS -> targetValue - modifierValue
			Operator.Kind.STAR_EQUALS -> targetValue * modifierValue
			Operator.Kind.SLASH_EQUALS -> {
				if(modifierValue.toDouble() == 0.0)
					return null
				targetValue / modifierValue
			}
			else -> throw CompilerError(source, "Static evaluation is not implemented for operators of kind '$kind'.")
		}
		return NumberLiteral(this, resultingValue)
	}

	override fun validate() {
		super.validate()
		validateWhereClauseConditions()
		validateMonomorphicAccess()
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

	private fun validateMonomorphicAccess() {
		val signature = targetSignature ?: return
		val valueType = target.providedType ?: return
		if(signature.associatedImplementation?.isAbstract == true && signature.associatedImplementation.isMonomorphic
			&& !valueType.isMemberAccessible(signature, true))
			context.addIssue(AbstractMonomorphicAccess(source, "operator",
				signature.toString(false, kind), valueType))
	}

	override fun toUnit() = BinaryModification(this, target.toUnit(), modifier.toUnit())
}
