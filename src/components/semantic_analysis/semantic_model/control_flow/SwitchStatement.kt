package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Value
import messages.Message
import components.syntax_parser.syntax_tree.control_flow.SwitchStatement as SwitchStatementSyntaxTree
import java.util.LinkedList

class SwitchStatement(override val source: SwitchStatementSyntaxTree, val subject: Value, val cases: List<Case>,
					  val elseBranch: Unit?): Unit(source) {
	override var isInterruptingExecution = false
	//TODO check if else branch is unnecessary, because the switch statement would be exhaustive without it

	init {
		addUnits(subject, elseBranch)
		addUnits(cases)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		for(case in cases) {
			if(case.condition.isAssignableTo(subject.type)) {
				case.condition.setInferredType(subject.type)
			} else {
				case.condition.type?.let { conditionType ->
					linter.addMessage(case.condition.source, "Condition type '$conditionType' " +
						"is not comparable to subject type '${subject.type}'.", Message.Type.ERROR)
				}
			}
		}
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		if(cases.isEmpty()) {
			linter.addMessage(source, "The switch statement doesn't have any cases.", Message.Type.WARNING)
		} else {
			val casesWithUniqueConditions = LinkedList<Case>()
			for(case in cases) {
				val caseWithUniqueCondition = casesWithUniqueConditions.find {
						caseWithUniqueCondition -> caseWithUniqueCondition.condition == case.condition }
				if(caseWithUniqueCondition != null) {
					linter.addMessage(case.condition.source,
						"Duplicated case '${case.condition.source.getValue()}'," +
							" previously defined in ${caseWithUniqueCondition.condition.source.getStartString()}.",
						Message.Type.ERROR)
					continue
				}
				casesWithUniqueConditions.add(case)
			}
		}
		//TODO implement pseudo code
//		isInterruptingExecution = (getBranchForValue(subject.staticValue)?.isInterruptingExecution ?: false)
//			|| (isExhaustive() && allBranchesInterruptExecution)
	}

	private fun isExhaustive(): Boolean {
		if(elseBranch != null)
			return true
		//TODO implement pseudo code
//		if(Linter.LiteralType.BOOLEAN.matches(subject.type))
//			return casesContainValue(true) && casesContainValue(false)
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
