package linter.elements.control_flow

import linter.elements.general.Unit
import parsing.ast.control_flow.SwitchStatement

class SwitchStatement(val source: SwitchStatement, val subject: Unit, val cases: List<Case>, val elseBranch: Unit?): Unit() {

	init {
		units.add(subject)
		units.addAll(cases)
		if(elseBranch != null)
			units.add(elseBranch)
	}
}