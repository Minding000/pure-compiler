package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.context.VariableUsage
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.SelfReference
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import logger.issues.constant_conditions.ExpressionNotAssignable
import logger.issues.constant_conditions.TypeNotAssignable
import logger.issues.initialization.ConstantReassignment
import logger.issues.resolution.ConversionAmbiguity
import components.syntax_parser.syntax_tree.operations.Assignment as AssignmentSyntaxTree

class Assignment(override val source: AssignmentSyntaxTree, scope: Scope, val targets: List<Value>, val sourceExpression: Value):
	Unit(source, scope) {
	var conversion: InitializerDefinition? = null

	init {
		addUnits(sourceExpression)
		addUnits(targets)
	}

	override fun determineTypes() {
		super.determineTypes()
		for(target in targets) {
			val targetType = target.type
			if(sourceExpression.isAssignableTo(targetType)) {
				sourceExpression.setInferredType(targetType)
				continue
			}
			val sourceType = sourceExpression.type ?: continue
			if(targetType == null) {
				target.type = sourceType
				continue
			}
			val conversions = targetType.getConversionsFrom(sourceType)
			if(conversions.isNotEmpty()) {
				if(conversions.size > 1) {
					context.addIssue(ConversionAmbiguity(source, sourceType, targetType, conversions))
					continue
				}
				conversion = conversions.first()
				continue
			}
			context.addIssue(TypeNotAssignable(source, sourceType, targetType))
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		sourceExpression.analyseDataFlow(tracker)
		for(target in targets) {
			when(target) {
				is VariableValue -> {
					tracker.add(VariableUsage.Kind.WRITE, target, sourceExpression.type, sourceExpression.getComputedValue(tracker))
					continue
				}
				is MemberAccess -> {
					if(target.member !is VariableValue || target.member.definition?.isConstant == true)
						context.addIssue(ConstantReassignment(source, target.member.toString()))
					if(target.target is SelfReference && target.member is VariableValue) {
						tracker.add(
							VariableUsage.Kind.WRITE, target.member, sourceExpression.type,
							sourceExpression.getComputedValue(tracker))
						continue
					}
				}
				is IndexAccess -> { //TODO is this tested?
					target.target.type?.interfaceScope?.resolveIndexOperator(target.typeParameters, target.indices, sourceExpression)
				}
				else -> { //TODO write test for this
					context.addIssue(ExpressionNotAssignable(target.source))
				}
			}
			target.analyseDataFlow(tracker)
		}
	}
}
