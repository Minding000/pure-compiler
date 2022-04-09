package linter.elements.control_flow

import linter.elements.general.Unit
import parsing.ast.control_flow.IfStatement

class IfStatement(val source: IfStatement, val trueBranch: Unit, val falseBranch: Unit?): Unit() {

	init {
		units.add(trueBranch)
		if(falseBranch != null)
			units.add(falseBranch)
	}
}