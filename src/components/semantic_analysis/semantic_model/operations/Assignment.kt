package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.VariableUsage
import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.SelfReference
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import messages.Message
import components.syntax_parser.syntax_tree.operations.Assignment as AssignmentSyntaxTree

class Assignment(override val source: AssignmentSyntaxTree, val targets: List<Value>, val sourceExpression: Value): Unit(source) {

	init {
		addUnits(sourceExpression)
		addUnits(targets)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		sourceExpression.linkValues(linter, scope)
		for(target in targets) {
			if(target is IndexAccess)
				target.sourceExpression = sourceExpression
			target.linkValues(linter, scope)
		}
		for(target in targets) {
			val targetType = target.type
			if(sourceExpression.isAssignableTo(targetType)) {
				sourceExpression.setInferredType(targetType)
			} else if(targetType == null) {
				target.type = sourceExpression.type
			} else {
				sourceExpression.type?.let { sourceType ->
					linter.addMessage(target.source, "Type '$sourceType' is not assignable to type '$targetType'.",
						Message.Type.ERROR)
				}
			}
		}
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		sourceExpression.analyseDataFlow(linter, tracker)
		for(target in targets) {
			when(target) {
				is VariableValue -> {
					val usage = tracker.add(VariableUsage.Type.WRITE, target) ?: continue
					val targetDeclaration = target.definition
					if(targetDeclaration?.isConstant == true) {
						if((targetDeclaration is PropertyDeclaration && !(tracker.isInitializer && targetDeclaration.value == null))
							|| usage.isPreviouslyPossiblyInitialized())
							linter.addMessage(target.source, "'${target.name}' cannot be reassigned, because it is constant.",
								Message.Type.ERROR)
					}
					continue
				}
				is MemberAccess -> { //TODO is this tested?
					if(target.member !is VariableValue || target.member.definition?.isConstant == true)
						linter.addMessage(target.source, "'${target.member}' cannot be reassigned, because it is constant.",
							Message.Type.ERROR)
					if(target.target is SelfReference && target.member is VariableValue) {
						tracker.add(VariableUsage.Type.WRITE, target.member)
						continue
					}
				}
				is IndexAccess -> { //TODO is this tested?
					target.target.type?.scope?.resolveIndexOperator(target.typeParameters, target.indices, sourceExpression)
				}
				else -> { //TODO write test for this
					linter.addMessage(target.source, "Expression is not assignable.", Message.Type.ERROR)
				}
			}
			target.analyseDataFlow(linter, tracker)
		}
	}
}
