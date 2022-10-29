package linting.semantic_model.control_flow

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.scopes.Scope
import linting.semantic_model.values.Value
import messages.Message
import parsing.syntax_tree.control_flow.SwitchStatement
import java.util.LinkedList

class SwitchStatement(override val source: SwitchStatement, val subject: Value, val cases: List<Case>,
					  val elseBranch: Unit?): Unit(source) {
	override var isInterruptingExecution = false //TODO adjust this value based on branches

	init {
		units.add(subject)
		units.addAll(cases)
		if(elseBranch != null)
			units.add(elseBranch)
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
		val casesWithUniqueConditions = LinkedList<Case>()
		for(case in cases) {
			val caseWithUniqueCondition = casesWithUniqueConditions.find {
					caseWithUniqueCondition -> caseWithUniqueCondition.condition == case.condition }
			if(caseWithUniqueCondition != null) {
				linter.addMessage(case.condition.source, "Duplicated case '${case.condition.source.getValue()}'," +
						" previously defined in ${caseWithUniqueCondition.condition.source.getStartString()}.",
					Message.Type.ERROR)
				continue
			}
			casesWithUniqueConditions.add(case)
		}
	}
}
