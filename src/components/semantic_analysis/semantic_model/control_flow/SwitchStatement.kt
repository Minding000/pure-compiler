package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.context.VariableUsage
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.BooleanLiteral
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import logger.issues.switches.CaseTypeMismatch
import logger.issues.switches.DuplicateCase
import logger.issues.switches.NoCases
import logger.issues.switches.RedundantElse
import java.util.*
import components.syntax_parser.syntax_tree.control_flow.SwitchStatement as SwitchStatementSyntaxTree

class SwitchStatement(override val source: SwitchStatementSyntaxTree, scope: Scope, val subject: Value, val cases: List<Case>,
					  val elseBranch: SemanticModel?): SemanticModel(source, scope) {
	override var isInterruptingExecution = false

	init {
		addSemanticModels(subject, elseBranch)
		addSemanticModels(cases)
	}

	override fun determineTypes() {
		super.determineTypes()
		val subjectType = subject.type
		for(case in cases) {
			if(case.condition.isAssignableTo(subjectType)) {
				case.condition.setInferredType(subjectType)
			} else {
				val conditionType = case.condition.type
				if(subjectType != null && conditionType != null)
					context.addIssue(CaseTypeMismatch(source, conditionType, subjectType))
			}
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		subject.analyseDataFlow(tracker)
		val caseStates = LinkedList<VariableTracker.VariableState>()
		val variableValue = subject as? VariableValue
		val subjectDeclaration = variableValue?.definition
		for(case in cases) {
			case.condition.analyseDataFlow(tracker)
			val negativeState = tracker.currentState.copy()
			if(subjectDeclaration != null) {
				tracker.add(VariableUsage.Kind.HINT, subjectDeclaration, case, case.condition.getComputedType(),
					case.condition.getComputedValue())
			}
			case.result.analyseDataFlow(tracker)
			caseStates.add(tracker.currentState.copy())
			tracker.setVariableStates(negativeState)
		}
		elseBranch?.analyseDataFlow(tracker)
		tracker.addVariableStates(caseStates)
	}

	override fun validate() {
		super.validate()
		var areAllBranchesInterruptingExecution = elseBranch?.isInterruptingExecution ?: true
		if(cases.isEmpty()) {
			context.addIssue(NoCases(source))
		} else {
			val casesWithUniqueConditions = LinkedList<Case>()
			for(case in cases) {
				if(!case.result.isInterruptingExecution)
					areAllBranchesInterruptingExecution = false
				val caseWithUniqueCondition = casesWithUniqueConditions.find {
						caseWithUniqueCondition -> caseWithUniqueCondition.condition == case.condition }
				if(caseWithUniqueCondition != null) {
					context.addIssue(DuplicateCase(caseWithUniqueCondition, case))
					continue
				}
				casesWithUniqueConditions.add(case)
			}
		}
		if(elseBranch != null && isExhaustiveWithoutElseBranch())
			context.addIssue(RedundantElse(elseBranch.source))
		isInterruptingExecution = (getBranchForValue(subject.getComputedValue())?.isInterruptingExecution ?: false)
			|| (isExhaustive() && areAllBranchesInterruptingExecution)
	}

	private fun getBranchForValue(value: Value?): SemanticModel? {
		for(case in cases) {
			if(case.condition.getComputedValue() == value)
				return case.result
		}
		return null
	}

	private fun isExhaustive(): Boolean {
		if(elseBranch != null)
			return true
		if(isExhaustiveWithoutElseBranch())
			return true
		return false
	}

	private fun isExhaustiveWithoutElseBranch(): Boolean {
		if(subject.getComputedValue() is BooleanLiteral) {
			var containsYes = false
			var containsNo = false
			for(case in cases) {
				if((case.condition as? BooleanLiteral)?.value == true)
					containsYes = true
				if((case.condition as? BooleanLiteral)?.value == false)
					containsNo = true
			}
			return containsYes && containsNo
		}
		//TODO detect exhausted enum values by implementing the following pseudo code:
//		if(subject.type.definition is Enum) {
//			for(instance in enum.instances) {
//				if(!casesContainValue(instance))
//					return false
//			}
//			return true
//		}
		return false
	}
}
