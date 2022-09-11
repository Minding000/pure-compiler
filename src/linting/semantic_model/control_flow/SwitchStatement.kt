package linting.semantic_model.control_flow

import linting.semantic_model.general.Unit
import parsing.syntax_tree.control_flow.SwitchStatement

class SwitchStatement(val source: SwitchStatement, val subject: Unit, val cases: List<Case>, val elseBranch: Unit?): Unit() {

	init {
		units.add(subject)
		units.addAll(cases)
		if(elseBranch != null)
			units.add(elseBranch)
	}
}