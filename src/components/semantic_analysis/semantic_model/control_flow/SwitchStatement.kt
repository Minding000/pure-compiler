package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.BooleanLiteral
import components.semantic_analysis.semantic_model.values.Value
import logger.issues.switches.CaseTypeMismatch
import logger.issues.switches.DuplicateCase
import logger.issues.switches.NoCases
import logger.issues.switches.RedundantElse
import java.util.*
import components.syntax_parser.syntax_tree.control_flow.SwitchStatement as SwitchStatementSyntaxTree

class SwitchStatement(override val source: SwitchStatementSyntaxTree, scope: Scope, val subject: Value, val cases: List<Case>,
					  val elseBranch: Unit?): Unit(source, scope) {
	override var isInterruptingExecution = false

	init {
		addUnits(subject, elseBranch)
		addUnits(cases)
	}

	override fun linkValues(linter: Linter) {
		super.linkValues(linter)
		val subjectType = subject.type
		for(case in cases) {
			if(case.condition.isAssignableTo(subjectType)) {
				case.condition.setInferredType(subjectType)
			} else {
				val conditionType = case.condition.type
				if(subjectType != null && conditionType != null)
					linter.addIssue(CaseTypeMismatch(source, conditionType, subjectType))
			}
		}
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		var areAllBranchesInterruptingExecution = elseBranch?.isInterruptingExecution ?: true
		if(cases.isEmpty()) {
			linter.addIssue(NoCases(source))
		} else {
			val casesWithUniqueConditions = LinkedList<Case>()
			for(case in cases) {
				if(!case.result.isInterruptingExecution)
					areAllBranchesInterruptingExecution = false
				val caseWithUniqueCondition = casesWithUniqueConditions.find {
						caseWithUniqueCondition -> caseWithUniqueCondition.condition == case.condition }
				if(caseWithUniqueCondition != null) {
					linter.addIssue(DuplicateCase(caseWithUniqueCondition, case))
					continue
				}
				casesWithUniqueConditions.add(case)
			}
		}
		if(elseBranch != null && isExhaustiveWithoutElseBranch())
			linter.addIssue(RedundantElse(elseBranch.source))
		isInterruptingExecution = (getBranchForValue(subject.staticValue)?.isInterruptingExecution ?: false)
			|| (isExhaustive() && areAllBranchesInterruptingExecution)
	}

	private fun getBranchForValue(value: Value?): Unit? {
		for(case in cases) {
			if(case.condition.staticValue == value)
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
		if(subject.staticValue is BooleanLiteral) {
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
