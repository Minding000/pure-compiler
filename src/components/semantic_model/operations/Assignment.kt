package components.semantic_model.operations

import components.code_generation.llvm.models.operations.Assignment
import components.semantic_model.context.ComparisonResult
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.types.OptionalType
import components.semantic_model.types.SelfType
import components.semantic_model.values.SelfReference
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue
import logger.issues.constant_conditions.ExpressionNotAssignable
import logger.issues.constant_conditions.TypeNotAssignable
import logger.issues.initialization.ConstantReassignment
import logger.issues.resolution.ConversionAmbiguity
import components.syntax_parser.syntax_tree.operations.Assignment as AssignmentSyntaxTree

class Assignment(override val source: AssignmentSyntaxTree, scope: Scope, val targets: List<Value>, val sourceExpression: Value):
	SemanticModel(source, scope) {
	var conversions = HashMap<Value, InitializerDefinition>()

	init {
		addSemanticModels(sourceExpression)
		addSemanticModels(targets)
	}

	override fun determineTypes() {
		super.determineTypes()
		for(target in targets) {
			if(target is IndexAccess)
				continue
			context.registerWrite(target)
			val targetType = target.providedType
			if(sourceExpression.isAssignableTo(targetType)) {
				sourceExpression.setInferredType(targetType)
				continue
			}
			val sourceType = sourceExpression.providedType ?: continue
			if(targetType == null) {
				target.providedType = sourceType
				continue
			}
			val possibleConversions = targetType.getConversionsFrom(sourceType)
			if(possibleConversions.isEmpty()) {
				context.addIssue(TypeNotAssignable(source, sourceType, targetType))
				continue
			}
			var mostSpecificConversion: InitializerDefinition? = null
			specificityPrecedenceLoop@ for(conversion in possibleConversions) {
				for(otherConversion in possibleConversions) {
					if(otherConversion === conversion)
						continue
					if(conversion.compareSpecificity(otherConversion) != ComparisonResult.HIGHER)
						continue@specificityPrecedenceLoop
				}
				target.setInferredType(conversion.getParameterTypeAt(0))
				mostSpecificConversion = conversion
			}
			if(mostSpecificConversion == null) {
				context.addIssue(ConversionAmbiguity(source, sourceType, targetType, possibleConversions))
				continue
			}
			conversions[target] = mostSpecificConversion
		}
		registerSelfTypeUsages()
	}

	private fun registerSelfTypeUsages() {
		val sourceType = sourceExpression.providedType
		val baseSourceType = if(sourceType is OptionalType) sourceType.baseType else sourceType
		if(baseSourceType !is SelfType) {
			val surroundingFunction = scope.getSurroundingFunction()
			for(target in targets) {
				val targetType = target.providedType
				val baseTargetType = if(targetType is OptionalType) targetType.baseType else targetType
				if(baseTargetType is SelfType)
					surroundingFunction?.usesOwnTypeAsSelf = true
			}
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		sourceExpression.analyseDataFlow(tracker)
		for(target in targets) {
			when(target) {
				is VariableValue -> {
					tracker.add(VariableUsage.Kind.WRITE, target, sourceExpression.providedType, sourceExpression.getComputedValue())
					continue
				}
				is MemberAccess -> {
					if(target.target is SelfReference && target.member is VariableValue) {
						tracker.add(VariableUsage.Kind.WRITE, target.member, sourceExpression.providedType,
							sourceExpression.getComputedValue())
						continue
					}
					if(target.member !is VariableValue || target.member.declaration?.isConstant == true)
						context.addIssue(ConstantReassignment(source, target.member.toString()))
				}
				is IndexAccess -> {}
				else -> context.addIssue(ExpressionNotAssignable(target.source))
			}
			target.analyseDataFlow(tracker)
		}
	}

	override fun toUnit() = Assignment(this, targets.map(Value::toUnit), sourceExpression.toUnit())
}
